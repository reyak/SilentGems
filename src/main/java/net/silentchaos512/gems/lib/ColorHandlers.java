package net.silentchaos512.gems.lib;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.util.IItemProvider;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.silentchaos512.gems.SilentGems;
import net.silentchaos512.gems.block.urn.BlockSoulUrn;
import net.silentchaos512.gems.init.ModBlocks;
import net.silentchaos512.gems.item.ChaosRune;
import net.silentchaos512.gems.item.EnchantmentToken;
import net.silentchaos512.gems.item.ReturnHomeCharm;
import net.silentchaos512.gems.item.SoulGem;

import java.util.Arrays;

public final class ColorHandlers {
    private ColorHandlers() {}

    public static void onBlockColors(ColorHandlerEvent.Block event) {
        BlockColors colors = event.getBlockColors();
        SilentGems.LOGGER.info("ColorHandlers#onBlockColors: {}", colors);
        if (colors == null) {
            SilentGems.LOGGER.error("BlockColors is null?", new IllegalStateException("wat?"));
            return;
        }

        registerBlocks(colors, BlockSoulUrn::getBlockColor, ModBlocks.soulUrn);
    }

    public static void onItemColors(ColorHandlerEvent.Item event) {
        ItemColors colors = event.getItemColors();
        SilentGems.LOGGER.info("ColorHandlers#onItemColors: {}", colors);
        if (colors == null) {
            SilentGems.LOGGER.error("ItemColors is null?", new IllegalStateException("wat?"));
            return;
        }

        registerItems(colors, BlockSoulUrn::getItemColor, ModBlocks.soulUrn);
        registerItems(colors, EnchantmentToken::getItemColor, EnchantmentToken.INSTANCE);

        // Return Home Charm
        registerItems(colors, ReturnHomeCharm::getColor, Arrays.stream(Gems.values())
                .map(Gems::getReturnHomeCharm)
                .toArray(IItemProvider[]::new));

        // Drawing Compass
//        itemColors.register((stack, tintIndex) -> tintIndex == 0 ? ModItems.drawingCompass.getColor(stack).getColor() : 0xFFFFFF, ModItems.drawingCompass);

        // Chaos Runes
        registerItems(colors, ChaosRune::getColor, ChaosRune.INSTANCE.get());

        // Holding Gem
//        itemColors.registerItemColorHandler((stack, tintIndex) -> {
//            if (tintIndex == 1) {
//                if (stack.hasTagCompound()) {
//                    int id = stack.getTagCompound().getShort(ItemHoldingGem.NBT_GEM_ID);
//                    if (id >= 0 && id < Gems.values().length)
//                        return Gems.values()[id].getColor();
//                }
//            } else if (tintIndex == 2) {
//                IBlockState state = ModItems.holdingGem.getBlockPlaced(stack);
//                if (state != null) {
//                    return 0xFFFFFF;
//                    // return state.getMapColor().colorValue;
//                }
//            }
//            return 0xFFFFFF;
//        }, ModItems.holdingGem);

        // Soul Gems
        registerItems(colors, SoulGem::getColor, SoulGem.INSTANCE.get());

        // Tool Soul
//        itemColors.registerItemColorHandler((stack, tintIndex) -> {
//            ToolSoul soul = ModItems.toolSoul.getSoul(stack);
//            if (soul == null)
//                return tintIndex == 1 ? 0xFF00FF : tintIndex == 2 ? 0x0 : 0xFFFFFF;
//
//            switch (tintIndex) {
//                case 0:
//                    float ratio = 0.5f + MathHelper.sin((float) ClientTickHandler.ticksInGame / 15) / 6;
//                    return Color.blend(soul.getPrimaryElement().color, soul.getSecondaryElement().color,
//                            ratio);
//                case 1:
//                    return soul.getPrimaryElement().color;
//                case 2:
//                    return soul.getSecondaryElement().color;
//                default:
//                    return 0xFFFFFF;
//            }
//        }, ModItems.toolSoul);
    }

    private static void registerBlocks(BlockColors handler, IBlockColor blockColor, Block... blocks) {
        try {
            handler.register(blockColor, blocks);
        } catch (NullPointerException ex) {
            SilentGems.LOGGER.error("Something went horribly wrong when registering block colors (Forge bug?)", ex);
        }
    }

    private static void registerItems(ItemColors handler, IItemColor itemColor, IItemProvider... items) {
        try {
            handler.register(itemColor, items);
        } catch (NullPointerException ex) {
            SilentGems.LOGGER.error("Something went horribly wrong when registering item colors (Forge bug?)", ex);
        }
    }
}
