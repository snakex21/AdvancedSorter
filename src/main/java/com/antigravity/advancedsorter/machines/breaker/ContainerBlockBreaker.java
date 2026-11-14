package com.antigravity.advancedsorter.machines.breaker;

import com.antigravity.advancedsorter.tiles.TileBasicEnergy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerBlockBreaker extends Container {

    private final TileBlockBreaker tile;
    private int lastEnergy = -1;

    public ContainerBlockBreaker(InventoryPlayer playerInv, TileBlockBreaker tile) {
        this.tile = tile;

        IItemHandler inventory = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);

        // Tile Inventory (3x3 grid)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlotToContainer(new SlotItemHandler(inventory, col + row * 3, 62 + col * 18, 17 + row * 18));
            }
        }

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
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        int currentEnergy = tile.getEnergyStored();
        if (currentEnergy != lastEnergy) {
            for (IContainerListener listener : this.listeners) {
                // Split into two 16-bit values for large numbers
                listener.sendWindowProperty(this, 0, currentEnergy & 0xFFFF);
                listener.sendWindowProperty(this, 1, (currentEnergy >> 16) & 0xFFFF);
            }
            lastEnergy = currentEnergy;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int data) {
        if (id == 0) {
            int current = tile.getEnergyStored();
            tile.setClientEnergy((current & 0xFFFF0000) | (data & 0xFFFF));
        } else if (id == 1) {
            int current = tile.getEnergyStored();
            tile.setClientEnergy((current & 0x0000FFFF) | ((data & 0xFFFF) << 16));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return !playerIn.isSpectator();
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            if (index < 9) { // From Tile to Player
                if (!this.mergeItemStack(itemstack1, 9, 45, true)) {
                    return ItemStack.EMPTY;
                }
            } else { // From Player to Tile
                if (!this.mergeItemStack(itemstack1, 0, 9, false)) {
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
}
