package net.silentchaos512.gems.item;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.registries.ForgeRegistries;
import net.silentchaos512.gems.client.key.KeyTracker;
import net.silentchaos512.gems.init.ModItemGroups;
import net.silentchaos512.lib.event.ClientTicks;
import net.silentchaos512.utils.Lazy;

import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;

public final class EnchantmentToken extends Item {
    public enum Icon implements IStringSerializable {
        ANY, ARMOR, BOW, EMPTY, FISHING_ROD, SWORD, TOOL, TRIDENT, UNKNOWN;

        @Override
        public String getName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private static final String NBT_ENCHANTMENTS = "TokenEnchantments";

    private static final Map<Enchantment, Integer> OUTLINE_COLOR_MAP = new HashMap<>();
    private static final Map<EnumEnchantmentType, Icon> MODELS_BY_TYPE = new EnumMap<>(EnumEnchantmentType.class);

    static {
        MODELS_BY_TYPE.put(EnumEnchantmentType.ALL, Icon.ANY);
        MODELS_BY_TYPE.put(EnumEnchantmentType.BREAKABLE, Icon.ANY);
        MODELS_BY_TYPE.put(EnumEnchantmentType.ARMOR, Icon.ARMOR);
        MODELS_BY_TYPE.put(EnumEnchantmentType.ARMOR_CHEST, Icon.ARMOR);
        MODELS_BY_TYPE.put(EnumEnchantmentType.ARMOR_FEET, Icon.ARMOR);
        MODELS_BY_TYPE.put(EnumEnchantmentType.ARMOR_HEAD, Icon.ARMOR);
        MODELS_BY_TYPE.put(EnumEnchantmentType.ARMOR_LEGS, Icon.ARMOR);
        MODELS_BY_TYPE.put(EnumEnchantmentType.WEARABLE, Icon.ARMOR);
        MODELS_BY_TYPE.put(EnumEnchantmentType.BOW, Icon.BOW);
        MODELS_BY_TYPE.put(EnumEnchantmentType.DIGGER, Icon.TOOL);
        MODELS_BY_TYPE.put(EnumEnchantmentType.FISHING_ROD, Icon.FISHING_ROD);
        MODELS_BY_TYPE.put(EnumEnchantmentType.TRIDENT, Icon.TRIDENT);
        MODELS_BY_TYPE.put(EnumEnchantmentType.WEAPON, Icon.SWORD);
    }

    public static final Lazy<EnchantmentToken> INSTANCE = Lazy.of(EnchantmentToken::new);

    private EnchantmentToken() {
        super(new Properties().group(ModItemGroups.UTILITY));
        addPropertyOverride(new ResourceLocation("model_index"), EnchantmentToken::getModel);
    }

    // ==============================================
    // Methods for "constructing" enchantment tokens.
    // ==============================================

    public static ItemStack construct(EnchantmentData data) {
        ItemStack stack = new ItemStack(INSTANCE.get());
        addEnchantment(stack, data);
        return stack;
    }

    public static void addEnchantment(ItemStack stack, Enchantment enchantment, int level) {
        addEnchantment(stack, new EnchantmentData(enchantment, level));
    }

    public static void addEnchantment(ItemStack stack, EnchantmentData data) {
        NBTTagList tagList = getEnchantments(stack);
        boolean needToAddEnchantment = true;
        ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(data.enchantment);

        for (int i = 0; i < tagList.size(); ++i) {
            NBTTagCompound tags = tagList.getCompound(i);
            ResourceLocation existingId = ResourceLocation.tryCreate(tags.getString("id"));
            if (existingId != null && existingId.equals(id)) {
                if (tags.getInt("lvl") < data.enchantmentLevel) {
                    tags.putShort("lvl", (short) data.enchantmentLevel);
                }

                needToAddEnchantment = false;
                break;
            }
        }

        if (needToAddEnchantment) {
            NBTTagCompound tags = new NBTTagCompound();
            tags.putString("id", String.valueOf(id));
            tags.putShort("lvl", (short) data.enchantmentLevel);
            tagList.add(tags);
        }

        stack.getOrCreateTag().put(NBT_ENCHANTMENTS, tagList);
    }

    public static NBTTagList getEnchantments(ItemStack stack) {
        NBTTagCompound tags = stack.getTag();
        return tags != null ? tags.getList(NBT_ENCHANTMENTS, 10) : new NBTTagList();
    }

    private static Map<Enchantment, Integer> getEnchantmentMap(ItemStack stack) {
        Map<Enchantment, Integer> map = new HashMap<>();
        NBTTagList tagList = getEnchantments(stack);

        for (int i = 0; i < tagList.size(); ++i) {
            NBTTagCompound tag = tagList.getCompound(i);
            ResourceLocation id = ResourceLocation.tryCreate(tag.getString("id"));
            Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(id);
            if (enchantment != null) {
                map.put(enchantment, tag.getInt("lvl"));
            }
        }

        return map;
    }

    // ========
    // Crafting
    // ========

    public static boolean applyTokenToTool(ItemStack token, ItemStack tool) {
        if (token.isEmpty() || tool.isEmpty()) {
            return false;
        }

        // Enchantments on token
        Map<Enchantment, Integer> enchantmentsOnToken = getEnchantmentMap(token);
        if (enchantmentsOnToken.isEmpty()) {
            return false;
        }

        // Enchantments on tool
        Map<Enchantment, Integer> enchantmentsOnTool = EnchantmentHelper.getEnchantments(tool);

        // Make sure all enchantments can apply to the tool
        for (Entry<Enchantment, Integer> entry : enchantmentsOnToken.entrySet()) {
            Enchantment ench = entry.getKey();
            // Valid for tool?
            if (!ench.canApply(tool)) {
                return false;
            }

            // Does new enchantment conflict with any existing ones?
            for (Enchantment enchTool : enchantmentsOnTool.keySet()) {
                if (!ench.equals(enchTool) && !ench.isCompatibleWith(enchTool)) {
                    return false;
                }
            }
        }

        // Appy enchantments to new copy of tool
        if (!mergeEnchantmentLists(enchantmentsOnToken, enchantmentsOnTool)) {
            return false;
        }
        EnchantmentHelper.setEnchantments(enchantmentsOnToken, tool);

        return true;
    }

    private static boolean mergeEnchantmentLists(Map<Enchantment, Integer> ench1, Map<Enchantment, Integer> ench2) {
        int level, newLevel;
        // Add enchantments from second list to first...
        for (Enchantment enchantment : ench2.keySet()) {
            level = newLevel = ench2.get(enchantment);
            // If first list contains the enchantment, try increasing the level.
            if (ench1.containsKey(enchantment)) {
                newLevel = ench1.get(enchantment) + level;
                // Level too high?
                if (newLevel > enchantment.getMaxLevel()) {
                    return false;
                }
            }
            ench1.put(enchantment, newLevel);
        }

        return true;
    }

    // =========================
    // Item and ItemSL overrides
    // =========================

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        Map<Enchantment, Integer> enchants = getEnchantmentMap(stack);

        if (enchants.size() == 1) {
            Enchantment ench = enchants.keySet().iterator().next();
            list.add(subText("maxLevel", ench.getMaxLevel()));

            // Recipe info
//            if (KeyTracker.isControlDown()) {
//                list.add(subText("materials"));
//                String recipeString = recipeMap.get(ench);
//                if (recipeString != null && !recipeString.isEmpty()) {
//                    for (String str : recipeString.split(";")) {
//                        list.add("  " + str);
//                    }
//                }
//            } else {
//                list.add(SilentGems.i18n.subText(this, "pressCtrl"));
//            }

            // Debug info
            if (KeyTracker.isAltDown()) {
                ResourceLocation registryName = Objects.requireNonNull(ench.getRegistryName());
                list.add(new TextComponentString(registryName.toString())
                        .applyTextStyle(TextFormatting.DARK_GRAY));
            }
        }

        // Enchantment list
        for (Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            Enchantment enchantment = entry.getKey();
            ITextComponent enchName = enchantment.getDisplayName(entry.getValue());
            ResourceLocation registryName = Objects.requireNonNull(enchantment.getRegistryName());
            String modName = "Unknown";
            for (ModInfo info : ModList.get().getMods()) {
                if (info.getModId().equals(registryName.getNamespace())) {
                    modName = info.getDisplayName();
                    break;
                }
            }
            list.add(subText("enchNameWithMod", enchName, modName));
            String descKey = enchantment.getName() + ".desc";
            if (I18n.hasKey(descKey)) {
                list.add(new TextComponentTranslation(descKey).applyTextStyle(TextFormatting.ITALIC));
            }
        }
    }

    private ITextComponent subText(String key, Object... formatArgs) {
        ResourceLocation id = Objects.requireNonNull(getRegistryName());
        String fullKey = String.format("item.%s.%s.%s", id.getNamespace(), id.getPath(), key);
        return new TextComponentTranslation(fullKey, formatArgs);
    }

    @Override
    public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items) {
        if (!isInGroup(group)) return;

        List<ItemStack> tokens = NonNullList.create();
        for (Enchantment enchantment : ForgeRegistries.ENCHANTMENTS) {
            tokens.add(construct(new EnchantmentData(enchantment, 1)));
        }

        // Sort by type, then enchantment name
        tokens.sort(EnchantmentToken::compareEnchantmentNames);
        items.addAll(tokens);
    }

    private static int compareEnchantmentNames(ItemStack o1, ItemStack o2) {
        // First compare icon names (group together enchantments of one type)
        int k = -getModelIcon(o1).getName().compareTo(getModelIcon(o2).getName());
        if (k == 0) {
            Enchantment ench1 = getSingleEnchantment(o1);
            Enchantment ench2 = getSingleEnchantment(o2);
            if (ench1 != null && ench2 != null) {
                // If this crashes the enchantment is at fault, nothing should be done about it.
                ITextComponent name1 = ench1.getDisplayName(1);
                ITextComponent name2 = ench2.getDisplayName(1);
                return name1.getFormattedText().compareTo(name2.getFormattedText());
            }
        }
        return k;
    }

    @Nullable
    public static Enchantment getSingleEnchantment(ItemStack token) {
        Map<Enchantment, Integer> map = getEnchantmentMap(token);
        if (map.size() != 1) return null;
        return map.keySet().iterator().next();
    }

    // =====================
    // = Rendering methods =
    // =====================

    @Override
    public boolean hasEffect(ItemStack stack) {
        return false;
    }

    private static float OUTLINE_PULSATE_SPEED = 1f / (3f * (float) Math.PI);

    public static int getItemColor(ItemStack stack, int tintIndex) {
        if (tintIndex != 1) return 0xFFFFFF;

        Enchantment ench = getSingleEnchantment(stack);
        if (ench != null && OUTLINE_COLOR_MAP.containsKey(ench)) {
            int k = OUTLINE_COLOR_MAP.get(ench);
            int r = (k >> 16) & 255;
            int g = (k >> 8) & 255;
            int b = k & 255;

            int j = (int) (160 * MathHelper.sin(ClientTicks.ticksInGame() * OUTLINE_PULSATE_SPEED));
            j = MathHelper.clamp(j, 0, 255);
            r = MathHelper.clamp(r + j, 0, 255);
            g = MathHelper.clamp(g + j, 0, 255);
            b = MathHelper.clamp(b + j, 0, 255);
            return (r << 16) | (g << 8) | b;
        }
        return 0x8040CC;
    }

    public static void setOutlineColor(Enchantment enchantment, int color) {
        OUTLINE_COLOR_MAP.put(enchantment, color);
    }

    private static float getModel(ItemStack stack, World world, EntityLivingBase entity) {
        return getModelIcon(stack).ordinal();
    }

    private static Icon getModelIcon(ItemStack stack) {
        Map<Enchantment, Integer> map = getEnchantmentMap(stack);
        if (map.isEmpty()) return Icon.EMPTY;
        return MODELS_BY_TYPE.getOrDefault(map.keySet().iterator().next().type, Icon.UNKNOWN);
    }

    private String getEnchantmentDebugInfo(Enchantment ench) {
        String str = ench.toString();
        str += "\n    Name: " + ench.getName();
        str += "\n    Registry Name: " + ench.getRegistryName();
        str += "\n    Name: " + ench.getName();
        str += "\n    Max Level: " + ench.getMaxLevel();
        str += "\n    Type: " + ench.type;
        str += "\n    Allowed On Books: " + ench.isAllowedOnBooks();
        str += "\n    Curse: " + ench.isCurse();
        str += "\n    Treasure: " + ench.isTreasureEnchantment();
        return str;
    }
}
