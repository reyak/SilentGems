/*
 * Silent's Gems -- BlockSoulUrn
 * Copyright (C) 2018 SilentChaos512
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 3
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.silentchaos512.gems.block.urn;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReaderBase;
import net.minecraft.world.World;
import net.silentchaos512.gems.client.gui.GuiTypes;
import net.silentchaos512.gems.client.key.KeyTracker;
import net.silentchaos512.gems.init.ModItemGroups;
import net.silentchaos512.gems.init.ModSounds;
import net.silentchaos512.gems.lib.Gems;
import net.silentchaos512.gems.lib.urn.LidState;
import net.silentchaos512.gems.lib.urn.UrnConst;
import net.silentchaos512.gems.lib.urn.UrnHelper;
import net.silentchaos512.gems.lib.urn.UrnUpgrade;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class BlockSoulUrn extends BlockContainer {
    private static final VoxelShape SHAPE_CLOSED = Block.makeCuboidShape(1, 0, 1, 15, 15, 15);
    private static final VoxelShape SHAPE_OPEN = Block.makeCuboidShape(1, 0, 1, 15, 14, 15);

    static final EnumProperty<LidState> LID = EnumProperty.create("lid", LidState.class);

    private static final EnumProperty<EnumFacing> FACING = DirectionProperty.create("facing", EnumFacing.Plane.HORIZONTAL);

    public BlockSoulUrn() {
        super(Properties.create(Material.ROCK)
                .hardnessAndResistance(5, 40));
        this.setDefaultState(this.getDefaultState()
                .with(FACING, EnumFacing.SOUTH)
                .with(LID, LidState.CLOSED));
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, IBlockState> builder) {
        builder.add(FACING, LID);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(IBlockReader worldIn) {
        return new TileSoulUrn();
    }

    public ItemStack getStack(int color, @Nullable Gems gem) {
        ItemStack stack = new ItemStack(this);
        if (color != UrnConst.UNDYED_COLOR) UrnHelper.setClayColor(stack, color);
        if (gem != null) UrnHelper.setGem(stack, gem);
        return stack;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable IBlockReader worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        int color = UrnHelper.getClayColor(stack);
        Gems gem = UrnHelper.getGem(stack);

        if (color != UrnConst.UNDYED_COLOR) {
            tooltip.add(translate("color", String.format("#%06X", color)));
        } else {
            tooltip.add(translate("color", translate("color.uncolored")));
        }

        if (gem != null) {
            tooltip.add(translate("gem", gem.getItemStack().getDisplayName()));
        }

        if (KeyTracker.isControlDown()) {
            tooltip.add(translate("upgrades").applyTextStyle(TextFormatting.YELLOW));
            List<UrnUpgrade> upgrades = UrnUpgrade.ListHelper.load(stack);
            for (UrnUpgrade upgrade : upgrades) {
                tooltip.add(translate("upgrade_list", upgrade.getDisplayName()));
            }
        } else {
            ITextComponent pressCtrl = new TextComponentTranslation("misc.silentgems.pressCtrl")
                    .applyTextStyle(TextFormatting.DARK_GRAY);
            tooltip.add(translate("upgrades", pressCtrl).applyTextStyle(TextFormatting.YELLOW));
        }
    }

    private static ITextComponent translate(String key, Object... args) {
        return new TextComponentTranslation("block.silentgems.soul_urn." + key, args);
    }

    @Override
    public void onBlockHarvested(World worldIn, BlockPos pos, IBlockState state, EntityPlayer player) {
        TileEntity tile = worldIn.getTileEntity(pos);
        if (tile instanceof TileSoulUrn) {
            TileSoulUrn tileSoulUrn = (TileSoulUrn) tile;
            tileSoulUrn.setDestroyedByCreativePlayer(player.abilities.isCreativeMode);
            tileSoulUrn.fillWithLoot(player);
        }
        super.onBlockHarvested(worldIn, pos, state, player);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void dropBlockAsItemWithChance(IBlockState state, World worldIn, BlockPos pos, float chancePerItem, int fortune) {
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onBlockActivated(IBlockState state, World worldIn, BlockPos pos, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            LidState lid = state.get(LID);

            if (lid != LidState.NO_LID && (player.isSneaking() || !lid.isOpen())) {
                // Toggle lid state when sneaking or if closed
                worldIn.setBlockState(pos, toggleLid(state), 2);
                ModSounds.SOUL_URN_LID.play(worldIn, pos);
            } else {
                // Open inventory if lid is open (or there is no lid)
                TileEntity tile = worldIn.getTileEntity(pos);
                if (tile instanceof TileSoulUrn) {
                    GuiTypes.SOUL_URN.display(player, worldIn, pos);
//                    player.displayGUIChest((TileSoulUrn) tile);
                    ModSounds.SOUL_URN_OPEN.play(worldIn, pos);
                }
            }
        }

        return true;
    }

    private static IBlockState toggleLid(IBlockState state) {
        LidState lid = state.get(LID);
        if (lid == LidState.NO_LID) return state;
        return state.with(LID, lid == LidState.CLOSED ? LidState.OPEN : LidState.CLOSED);
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, @Nullable EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        EnumFacing side = placer != null
                ? placer.getHorizontalFacing().getOpposite()
                : EnumFacing.SOUTH;
        LidState lid = LidState.fromItem(stack);
        IBlockState newState = state.with(FACING, side).with(LID, lid);

        worldIn.setBlockState(pos, newState, 2);

        TileEntity tile = worldIn.getTileEntity(pos);
        if (tile instanceof TileSoulUrn) {
            TileSoulUrn tileSoulUrn = (TileSoulUrn) tile;
            if (stack.hasDisplayName()) {
                tileSoulUrn.setCustomName(stack.getDisplayName());
            }

            tileSoulUrn.setColorAndGem(UrnHelper.getClayColor(stack), UrnHelper.getGem(stack));

            NBTTagCompound tagCompound = stack.getOrCreateChildTag(UrnConst.NBT_ROOT);
            tileSoulUrn.setUpgrades(UrnUpgrade.ListHelper.load(tagCompound));
        }
    }

    @Override
    public void onReplaced(IBlockState state, World worldIn, BlockPos pos, IBlockState newState, boolean isMoving) {
        TileEntity tile = worldIn.getTileEntity(pos);

        if (state.getBlock() != newState.getBlock() && tile instanceof TileSoulUrn) {
            TileSoulUrn tileSoulUrn = (TileSoulUrn) tile;

            if (!tileSoulUrn.isCleared() && tileSoulUrn.shouldDrop()) {
                ItemStack stack = new ItemStack(this);
                UrnHelper.setHasLid(stack, state.get(LID).hasLid());

                NBTTagCompound compound = new NBTTagCompound();
                NBTTagCompound compound1 = new NBTTagCompound();
                compound.put(UrnConst.NBT_ROOT, tileSoulUrn.saveToNBT(compound1));
                stack.setTag(compound);

                if (tileSoulUrn.hasCustomName()) {
                    stack.setDisplayName(tileSoulUrn.getName());
                    tileSoulUrn.setCustomName(new TextComponentString(""));
                }

                if (tileSoulUrn.getColor() != UrnConst.UNDYED_COLOR)
                    UrnHelper.setClayColor(stack, tileSoulUrn.getColor());
                if (tileSoulUrn.getGem() != null)
                    UrnHelper.setGem(stack, tileSoulUrn.getGem());

                spawnAsEntity(worldIn, pos, stack);
            }

            worldIn.updateComparatorOutputLevel(pos, state.getBlock());
        }

        super.onReplaced(state, worldIn, pos, newState, isMoving);
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, IBlockReader world, BlockPos pos, EntityPlayer player) {
        ItemStack stack = super.getPickBlock(state, target, world, pos, player);

        TileSoulUrn tileSoulUrn = (TileSoulUrn) world.getTileEntity(pos);
        if (tileSoulUrn != null) {
            NBTTagCompound compound = tileSoulUrn.saveToNBT(new NBTTagCompound());

            if (!compound.isEmpty())
                stack.setTagInfo("BlockEntityInfo", compound);

            UrnHelper.setClayColor(stack, tileSoulUrn.getColor());
            if (tileSoulUrn.getGem() != null)
                UrnHelper.setGem(stack, tileSoulUrn.getGem());
        }

        return stack;
    }

    @SuppressWarnings("TypeMayBeWeakened")
    public static int getBlockColor(IBlockState state, @Nullable IWorldReaderBase world, @Nullable BlockPos pos, int tintIndex) {
        if (tintIndex == 0) {
            // Main body/clay color
            if (world != null && pos != null) {
                TileEntity tile = world.getTileEntity(pos);
                if (tile instanceof TileSoulUrn) {
                    return ((TileSoulUrn) tile).getColor();
                }
            }
            // Fallback to plain hardened clay color
            return UrnConst.UNDYED_COLOR;
        } else if (tintIndex == 1) {
            // Decorative gem color
            if (world != null && pos != null) {
                TileEntity tile = world.getTileEntity(pos);
                if (tile instanceof TileSoulUrn) {
                    Gems gem = ((TileSoulUrn) tile).getGem();
                    if (gem != null) return gem.getColor();
                }
            }
            // Fall through to white if gem is null
        }
        return 0xFFFFFF;
    }

    public static int getItemColor(ItemStack stack, int tintIndex) {
        if (tintIndex == 0) {
            // Main body/clay color
            return UrnHelper.getClayColor(stack);
        } else if (tintIndex == 1) {
            // Decorative gem color
            Gems gem = UrnHelper.getGem(stack);
            return gem != null ? gem.getColor() : 0xFFFFFF;
        }
        return 0xFFFFFF;
    }

    @SuppressWarnings("deprecation")
    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(IBlockState state, IBlockReader worldIn, BlockPos pos) {
        return state.get(LID).isOpen() ? SHAPE_OPEN : SHAPE_CLOSED;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean causesSuffocation(IBlockState state) {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isSolid(IBlockState state) {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isNormalCube(IBlockState state, IBlockReader world, BlockPos pos) {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos) {
        return Container.calcRedstoneFromInventory((IInventory) worldIn.getTileEntity(pos));
    }

    @Nullable
    @Override
    public IBlockState getStateForPlacement(BlockItemUseContext context) {
        if (context.replacingClickedOnBlock()) {
            IBlockState currentState = context.getWorld().getBlockState(context.getPos());
            return toggleLid(currentState);
        }

        return this.getDefaultState().with(FACING, context.getPlacementHorizontalFacing());
    }

    public static class ItemBlockSoulUrn extends ItemBlock {
        private static List<ItemStack> SAMPLE_SUB_ITEMS;

        private final BlockSoulUrn blockSoulUrn;

        public ItemBlockSoulUrn(BlockSoulUrn block) {
            super(block, new Item.Properties()
                    .maxStackSize(1)
                    .group(ModItemGroups.BLOCKS));

            this.blockSoulUrn = block;
        }

        @Override
        public EnumActionResult onItemUse(ItemUseContext context) {
            IBlockState state = context.getWorld().getBlockState(context.getPos());
            // Cauldrons can remove dye color
            if (state.getBlock() == Blocks.CAULDRON) {
                int waterLevel = state.get(BlockCauldron.LEVEL);
                if (waterLevel > 0) {
                    UrnHelper.setClayColor(context.getItem(), UrnConst.UNDYED_COLOR);
                    ((BlockCauldron) Blocks.CAULDRON).setWaterLevel(context.getWorld(), context.getPos(), state, waterLevel - 1);
                    return EnumActionResult.SUCCESS;
                }

                return EnumActionResult.PASS;
            }

            return super.onItemUse(context);
        }

        @Override
        public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
            super.inventoryTick(stack, worldIn, entityIn, itemSlot, isSelected);
            // TODO: Tick upgrades
        }

        @Override
        public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items) {
            if (!this.isInGroup(group)) return;

            // Just show one of each clay color
            if (SAMPLE_SUB_ITEMS == null) {
                SAMPLE_SUB_ITEMS = new ArrayList<>();

                SAMPLE_SUB_ITEMS.add(this.blockSoulUrn.getStack(UrnConst.UNDYED_COLOR, Gems.selectRandom()));
                for (EnumDyeColor color : EnumDyeColor.values()) {
                    SAMPLE_SUB_ITEMS.add(this.blockSoulUrn.getStack(color.func_196057_c(), Gems.selectRandom()));
                }
            }

            items.addAll(SAMPLE_SUB_ITEMS);
        }

//        @Override
//        public String getTranslationKey(ItemStack stack) {
//            return super.getTranslationKey(stack)
//                    + (stack.getItemDamage() >> 2 == LidState.NO_LID.ordinal() ? "_no_lid" : "");
//        }
    }
}
