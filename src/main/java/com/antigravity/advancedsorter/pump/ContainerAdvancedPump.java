package com.antigravity.advancedsorter.pump;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;

public class ContainerAdvancedPump extends Container {

    private final TileAdvancedPump tile;
    private int lastEnergy;
    private int lastRate;

    public ContainerAdvancedPump(InventoryPlayer playerInventory, TileAdvancedPump tile) {
        this.tile = tile;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        if (lastEnergy != tile.getEnergyStored()) {
            for (int i = 0; i < this.listeners.size(); ++i) {
                IContainerListener icontainerlistener = this.listeners.get(i);
                icontainerlistener.sendWindowProperty(this, 0, tile.getEnergyStored() & 0xFFFF);
                icontainerlistener.sendWindowProperty(this, 1, (tile.getEnergyStored() >> 16) & 0xFFFF);
            }
            lastEnergy = tile.getEnergyStored();
        }

        if (lastRate != tile.getLastSecondRate()) {
            for (int i = 0; i < this.listeners.size(); ++i) {
                IContainerListener icontainerlistener = this.listeners.get(i);
                icontainerlistener.sendWindowProperty(this, 2, tile.getLastSecondRate() & 0xFFFF);
                icontainerlistener.sendWindowProperty(this, 3, (tile.getLastSecondRate() >> 16) & 0xFFFF);
            }
            lastRate = tile.getLastSecondRate();
        }
    }

    @Override
    @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public void updateProgressBar(int id, int data) {
        if (id == 0) {
            tile.setEnergyStored((tile.getEnergyStored() & 0xFFFF0000) | (data & 0xFFFF));
        } else if (id == 1) {
            tile.setEnergyStored((tile.getEnergyStored() & 0x0000FFFF) | ((data & 0xFFFF) << 16));
        } else if (id == 2) {
            setClientRate((tile.getLastSecondRate() & 0xFFFF0000) | (data & 0xFFFF));
        } else if (id == 3) {
            setClientRate((tile.getLastSecondRate() & 0x0000FFFF) | ((data & 0xFFFF) << 16));
        }
    }

    private void setClientRate(int rate) {
        try {
            java.lang.reflect.Field field = TileAdvancedPump.class.getDeclaredField("lastSecondRate");
            field.setAccessible(true);
            field.setInt(tile, rate);
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return playerIn.getDistanceSq(tile.getPos()) <= 64;
    }

    public TileAdvancedPump getTile() {
        return tile;
    }
}
