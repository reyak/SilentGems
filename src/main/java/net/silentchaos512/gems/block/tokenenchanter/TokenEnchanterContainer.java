package net.silentchaos512.gems.block.tokenenchanter;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.silentchaos512.lib.inventory.SlotOutputOnly;

import javax.annotation.Nonnull;

public class TokenEnchanterContainer extends Container {
    private final TokenEnchanterTileEntity tileEntity;

    public TokenEnchanterContainer(InventoryPlayer playerInventory, TokenEnchanterTileEntity tileEntity) {
        this.tileEntity = tileEntity;

        // Token slot
        this.addSlot(new Slot(this.tileEntity, 0, 22, 35));
        // Other ingredients slot
        this.addSlot(new Slot(this.tileEntity, 1, 48, 25));
        this.addSlot(new Slot(this.tileEntity, 2, 66, 25));
        this.addSlot(new Slot(this.tileEntity, 3, 84, 25));
        this.addSlot(new Slot(this.tileEntity, 4, 48, 43));
        this.addSlot(new Slot(this.tileEntity, 5, 66, 43));
        this.addSlot(new Slot(this.tileEntity, 6, 84, 43));
        // Output
        this.addSlot(new SlotOutputOnly(this.tileEntity, 7, 132, 35));

        int i;
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return this.tileEntity.isUsableByPlayer(playerIn);
    }

    @Nonnull
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(slotIndex);

        if (slot != null && slot.getHasStack()) {
            ItemStack stack1 = slot.getStack();
            stack = stack1.copy();

            final int playerStart = 5;
            final int playerEnd = playerStart + 36;

            if (slotIndex == 4) {
                // Transfer from output slot
                if (!this.mergeItemStack(stack1, playerStart, playerEnd, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onSlotChange(stack1, stack);
            } else if (slotIndex > 4) {
                // Transfer from player
                if (this.tileEntity.isItemValidForSlot(0, stack1)) {
                    // Blank tokens slot
                    if (!this.mergeItemStack(stack1, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (this.tileEntity.isItemValidForSlot(1, stack1)) {
                    // Ingredients start
                    if (!this.mergeItemStack(stack1, 1, 4, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // Move from hotbar to backpack or vice versa
                    final int hotbarStart = playerStart + 27;
                    if (slotIndex < hotbarStart) {
                        if (!this.mergeItemStack(stack1, hotbarStart, playerEnd, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (slotIndex < playerEnd && !this.mergeItemStack(stack1, playerStart, hotbarStart, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (!this.mergeItemStack(stack1, playerStart, playerEnd, false)) {
                return ItemStack.EMPTY;
            }

            if (stack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (stack1.getCount() == stack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack1);
        }

        return stack;
    }
}
