package com.antigravity.advancedsorter.machines.trashcan;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerTrashCan extends Container {

    private final TileTrashCan tile;

    public ContainerTrashCan(InventoryPlayer playerInv, TileTrashCan tile) {
        this.tile = tile;

        // Trash Slot - ID 0
        // We use a custom slot that voids items placed in it
        addSlotToContainer(new Slot(new net.minecraft.inventory.InventoryBasic("trash", false, 1), 0, 80, 35) {
            @Override
            public void putStack(ItemStack stack) {
                // Set to empty immediately? Or let it sit there?
                // If we set it to empty, it vanishes instantly.
                // Let's vanish it.
                this.inventory.setInventorySlotContents(0, ItemStack.EMPTY);
                this.onSlotChanged();
            }
        });

        // Player Inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return !playerIn.isSpectator();
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        // Shift-clicking into trash can voids the item
        Slot slot = this.inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            if (index > 0) { // From Player to Trash
                slot.putStack(ItemStack.EMPTY); // VOID IT
                slot.onSlotChanged();
            }
        }
        return ItemStack.EMPTY;
    }
}
