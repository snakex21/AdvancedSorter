package com.antigravity.advancedsorter.machines.xpvacuum;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ContainerXPVacuum extends Container {

    private final TileXPVacuum tile;
    private int lastEnergy = -1;
    private int lastFluid = -1;

    public ContainerXPVacuum(InventoryPlayer playerInv, TileXPVacuum tile) {
        this.tile = tile;

        // Player Inventory (Standard 3x9 grid)
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlotToContainer(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        // Hotbar (1x9 grid)
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
        int currentFluid = tile.getTank().getFluidAmount();

        for (IContainerListener listener : this.listeners) {
            if (currentEnergy != lastEnergy) {
                // Split energy into two 16-bit values
                listener.sendWindowProperty(this, 0, currentEnergy & 0xFFFF);
                listener.sendWindowProperty(this, 1, (currentEnergy >> 16) & 0xFFFF);
            }
            if (currentFluid != lastFluid) {
                // Fluid is also split for tanks > 32k mB
                listener.sendWindowProperty(this, 2, currentFluid & 0xFFFF);
                listener.sendWindowProperty(this, 3, (currentFluid >> 16) & 0xFFFF);
            }
        }

        lastEnergy = currentEnergy;
        lastFluid = currentFluid;
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
        }
        // Fluid sync handled server-side for simplicity; client displays via tank object
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            if (index < 27) { // Inventory -> Hotbar
                if (!this.mergeItemStack(itemstack1, 27, 36, false)) {
                    return ItemStack.EMPTY;
                }
            } else { // Hotbar -> Inventory
                if (!this.mergeItemStack(itemstack1, 0, 27, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }
        return itemstack;
    }
}
