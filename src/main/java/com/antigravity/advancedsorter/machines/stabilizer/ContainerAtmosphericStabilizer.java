package com.antigravity.advancedsorter.machines.stabilizer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerAtmosphericStabilizer extends Container {

    private final TileAtmosphericStabilizer tile;
    private int lastEnergy;
    private int lastWater;

    public ContainerAtmosphericStabilizer(InventoryPlayer playerInv, TileAtmosphericStabilizer tile) {
        this.tile = tile;

        IItemHandler inventory = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);

        // Slot 0: Bucket Input (125, 20)
        addSlotToContainer(new SlotItemHandler(inventory, 0, 125, 20));

        // Slot 1: Bucket Output (125, 50)
        addSlotToContainer(new SlotItemHandler(inventory, 1, 125, 50));

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

            if (index < 2) { // Machine Slots (0, 1) -> Player
                if (!this.mergeItemStack(itemstack1, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            } else { // Player -> Machine
                // If it's a fluid container, try Slot 0
                // (Simple check: is it a bucket?)
                // NOTE: Proper check would use FluidUtil, but we don't have world/pos easily here without tile access helper
                // Assuming everything goes to input
                if (!this.mergeItemStack(itemstack1, 0, 1, false)) {
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
        }

        this.lastEnergy = this.tile.getEnergyStored();
        this.lastWater = this.tile.getWaterAmount();
    }

    @Override
    public void updateProgressBar(int id, int data) {
        if (id == 0) this.tile.setEnergyStored(data);
        if (id == 1) this.tile.setWaterAmount(data);
    }
}
