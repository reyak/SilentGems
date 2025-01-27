package net.silentchaos512.gems.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.silentchaos512.gems.api.chaos.ChaosEmissionRate;
import net.silentchaos512.gems.client.key.KeyTracker;
import net.silentchaos512.gems.init.ModItemGroups;
import net.silentchaos512.gems.lib.chaosbuff.ChaosBuffManager;
import net.silentchaos512.gems.lib.chaosbuff.IChaosBuff;
import net.silentchaos512.gems.lib.chaosbuff.PotionChaosBuff;
import net.silentchaos512.utils.Color;
import net.silentchaos512.utils.Lazy;

import javax.annotation.Nullable;
import java.util.List;

public final class ChaosRune extends Item {
    public static final Lazy<ChaosRune> INSTANCE = Lazy.of(ChaosRune::new);

    private static final String NBT_KEY = "SGems_BuffRune";

    private ChaosRune() {
        super(new Properties().group(ModItemGroups.UTILITY));
    }

    public static ItemStack getStack(IChaosBuff buff) {
        ItemStack result = new ItemStack(INSTANCE.get());
        result.getOrCreateTag().putString(NBT_KEY, buff.getId().toString());
        return result;
    }

    @Nullable
    public static IChaosBuff getBuff(ItemStack stack) {
        String string = stack.getOrCreateTag().getString(NBT_KEY);
        return ChaosBuffManager.get(string);
    }

    public static int getColor(ItemStack stack, int tintIndex) {
        if (tintIndex != 1) return Color.VALUE_WHITE;
        IChaosBuff buff = getBuff(stack);
        if (buff == null) return Color.VALUE_WHITE;
        return buff.getRuneColor();
    }

    @Override
    public ITextComponent getDisplayName(ItemStack stack) {
        IChaosBuff buff = getBuff(stack);
        if (buff != null) {
            ITextComponent buffName = buff.getDisplayName(0);
            return new TextComponentTranslation(this.getTranslationKey() + ".nameProper", buffName);
        }
        return super.getDisplayName(stack);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        IChaosBuff buff = getBuff(stack);
        if (buff == null) return;

        tooltip.add(new TextComponentTranslation(this.getTranslationKey() + ".maxLevel", buff.getMaxLevel()));
        tooltip.add(new TextComponentTranslation(this.getTranslationKey() + ".slotsUsed", buff.getSlotsForLevel(1)));
        int activeChaosGenerated = buff.getActiveChaosGenerated(1);
        ChaosEmissionRate emissionRate = ChaosEmissionRate.fromAmount(activeChaosGenerated);
        tooltip.add(new TextComponentTranslation(this.getTranslationKey() + ".chaos", emissionRate.getDisplayName(activeChaosGenerated)));

        // Debug
        if (KeyTracker.isAltDown()) {
            tooltip.add(new TextComponentString(String.format("Buff ID: %s", buff.getId())).applyTextStyle(TextFormatting.DARK_GRAY));
            tooltip.add(new TextComponentString(String.format("Color: %X", buff.getRuneColor())).applyTextStyle(TextFormatting.DARK_GRAY));
            if (buff instanceof PotionChaosBuff) {
                Potion potion = ((PotionChaosBuff) buff).getPotion();
                tooltip.add(new TextComponentString(String.format("Potion: %s", potion)).applyTextStyle(TextFormatting.DARK_GRAY));
            }
        }
    }

    @Override
    public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items) {
        if (!isInGroup(group)) return;
        for (IChaosBuff buff : ChaosBuffManager.getValues()) {
            items.add(getStack(buff));
        }
    }
}
