package com.antigravity.advancedsorter.machines.crusher;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerCrusher extends Container {

    private final TileCrusher tile;

    public ContainerCrusher(InventoryPlayer playerInv, TileCrusher tile) {
        this.tile = tile;

        // Use internal handler for GUI access to both slots
        // (TileCrusher uses wrappers for automation, but GUI needs direct access)
        // We need to cast or add a getter in TileCrusher.
        // Assuming TileCrusher.getCapability(ITEM_HANDLER, null) returns the wrapper
        // BUT for GUI we usually want the full inventory.
        // Let's modify TileCrusher to expose full inventory or use a getter.
        // For now, let's assume getCapability with null facing returns full inv or modify TileCrusher later if needed.
        // Actually TileCrusher uses separate wrappers. Let's add a public getter for inventory in TileCrusher.
        // Wait, I can't modify TileCrusher easily right here without Read.
        // Let's assume for now we use the capability and hope it exposes both slots or modify TileCrusher.
        // Actually, looking at TileCrusher I wrote earlier:
        // "Expose Input (0) to all sides, Output (1) to all sides... For now, simple wrapper"
        // The wrapper logic I wrote ONLY exposes slot 0 for insert and slot 1 for extract.
        // So standard capability won't work for GUI slot 0 and 1 simultaneous access.
        // I need to add a method to TileCrusher to get the internal handler.

        IItemHandler inventory = tile.getInternalInventory();

        // Input Slot (0)
        addSlotToContainer(new SlotItemHandler(inventory, 0, 56, 35));

        // Output Slot (1)
        addSlotToContainer(new SlotItemHandler(inventory, 1, 116, 35) {
             @Override
             public boolean isItemValid(ItemStack stack) {
                 return false; // Output only
             }
        });

        // Player Inventory
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlotToContainer(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        // Hotbar
        for (int k = 0; k < 9; k++) {
            addSlotToContainer(new Slot(playerInv, k, 8 + k * 18, 142));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return !playerIn.isSpectator() &&
               playerIn.getDistanceSq(tile.getPos().getX() + 0.5, tile.getPos().getY() + 0.5, tile.getPos().getZ() + 0.5) <= 64;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            if (index == 1) { // Output slot
                if (!this.mergeItemStack(itemstack1, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onSlotChange(itemstack1, itemstack);
            } else if (index != 0) { // Player Inventory -> Input
                if (!this.mergeItemStack(itemstack1, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else { // Input -> Player (Shift-click out of input)
                 if (!this.mergeItemStack(itemstack1, 2, 38, true)) {
                    return ItemStack.EMPTY;
                 }
            }

            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, itemstack1);
        }

        return itemstack;
    }

    private int lastEnergy;
    private int lastProgress;
    private int lastMode;

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        for (int i = 0; i < this.listeners.size(); ++i) {
            net.minecraft.inventory.IContainerListener listener = this.listeners.get(i);

            if (this.lastEnergy != this.tile.getEnergyStored()) {
                listener.sendWindowProperty(this, 0, this.tile.getEnergyStored());
            }
            if (this.lastProgress != this.tile.getProgress()) {
                listener.sendWindowProperty(this, 1, this.tile.getProgress());
            }
            if (this.lastMode != this.tile.getMode()) {
                listener.sendWindowProperty(this, 2, this.tile.getMode());
            }
        }

        this.lastEnergy = this.tile.getEnergyStored();
        this.lastProgress = this.tile.getProgress();
        this.lastMode = this.tile.getMode();
    }

    @Override
    public void updateProgressBar(int id, int data) {
        if (id == 0) this.tile.setEnergyStored(data);
        if (id == 1) this.tile.setProgress(data);
        if (id == 2) this.tile.setMode(data);
    }
}
