package com.antigravity.advancedsorter.machines.cobblegen;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerCobbleGenerator extends Container {

    private final TileCobbleGenerator tile;

    public ContainerCobbleGenerator(InventoryPlayer playerInv, TileCobbleGenerator tile) {
        this.tile = tile;

        IItemHandler inventory = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);

        // Output Slot (0)
        addSlotToContainer(new SlotItemHandler(inventory, 0, 80, 35) {
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

            if (index == 0) { // Output slot
                if (!this.mergeItemStack(itemstack1, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onSlotChange(itemstack1, itemstack);
            } else { // Player Inventory
                // CobbleGen doesn't accept items, so no merge into machine
                return ItemStack.EMPTY;
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
    private int lastWater;
    private int lastLava;
    private int lastProgress;

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        for (int i = 0; i < this.listeners.size(); ++i) {
            net.minecraft.inventory.IContainerListener listener = this.listeners.get(i);

            if (this.lastEnergy != this.tile.getEnergyStored()) {
                listener.sendWindowProperty(this, 0, this.tile.getEnergyStored());
            }
            if (this.lastWater != this.tile.getWaterAmount()) {
                listener.sendWindowProperty(this, 1, this.tile.getWaterAmount());
            }
            if (this.lastLava != this.tile.getLavaAmount()) {
                listener.sendWindowProperty(this, 2, this.tile.getLavaAmount());
            }
            if (this.lastProgress != this.tile.getProgress()) {
                listener.sendWindowProperty(this, 3, this.tile.getProgress());
            }
        }

        this.lastEnergy = this.tile.getEnergyStored();
        this.lastWater = this.tile.getWaterAmount();
        this.lastLava = this.tile.getLavaAmount();
        this.lastProgress = this.tile.getProgress();
    }

    @Override
    public void updateProgressBar(int id, int data) {
        if (id == 0) this.tile.setEnergyStored(data);
        if (id == 1) this.tile.setWaterAmount(data);
        if (id == 2) this.tile.setLavaAmount(data);
        if (id == 3) this.tile.setProgress(data);
    }
}
