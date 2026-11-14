package com.antigravity.advancedsorter.machines.extractor;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerEnchantmentExtractor extends Container {

    private final TileEnchantmentExtractor tile;
    private int lastEnergy = -1;
    private int lastProgress = -1;

    public ContainerEnchantmentExtractor(InventoryPlayer playerInv, TileEnchantmentExtractor tile) {
        this.tile = tile;
        IItemHandler inventory = tile.getInventory();

        // Input Slots (Left)
        addSlotToContainer(new SlotItemHandler(inventory, 0, 44, 26)); // Enchanted Item
        addSlotToContainer(new SlotItemHandler(inventory, 1, 44, 52)); // Book

        // Output Slots (Right)
        addSlotToContainer(new SlotItemHandler(inventory, 2, 116, 26) { // Clean Item
            @Override
            public boolean isItemValid(ItemStack stack) { return false; }
        });
        addSlotToContainer(new SlotItemHandler(inventory, 3, 116, 52) { // Enchanted Book
            @Override
            public boolean isItemValid(ItemStack stack) { return false; }
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
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        int currentEnergy = tile.getEnergyStored();
        int currentProgress = tile.getProgress();

        for (IContainerListener listener : this.listeners) {
            if (currentEnergy != lastEnergy) {
                // Split energy into two 16-bit values
                listener.sendWindowProperty(this, 0, currentEnergy & 0xFFFF);
                listener.sendWindowProperty(this, 1, (currentEnergy >> 16) & 0xFFFF);
            }
            if (currentProgress != lastProgress) {
                listener.sendWindowProperty(this, 2, currentProgress);
            }
        }

        lastEnergy = currentEnergy;
        lastProgress = currentProgress;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int data) {
        if (id == 0) {
            int current = tile.getEnergyStored();
            tile.setEnergyStored((current & 0xFFFF0000) | (data & 0xFFFF));
        } else if (id == 1) {
            int current = tile.getEnergyStored();
            tile.setEnergyStored((current & 0x0000FFFF) | ((data & 0xFFFF) << 16));
        } else if (id == 2) {
            tile.setProgress(data);
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            if (index < 4) { // Machine Slots (0-3) -> Player
                if (!this.mergeItemStack(itemstack1, 4, 40, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onSlotChange(itemstack1, itemstack);
            } else { // Player -> Machine
                // Try Input Item (Slot 0)
                if (tile.getInventory().isItemValid(0, itemstack1)) {
                    if (!this.mergeItemStack(itemstack1, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // Try Input Book (Slot 1)
                else if (tile.getInventory().isItemValid(1, itemstack1)) {
                    if (!this.mergeItemStack(itemstack1, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
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
}
