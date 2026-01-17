package com.antigravity.advancedsorter.tanks;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;

public class ContainerFluidTank extends Container {

    private final TileFluidTank tile;

    public ContainerFluidTank(InventoryPlayer playerInv, TileFluidTank tile) {
        this.tile = tile;
        // Notify tile that GUI is open - enables frequent sync
        tile.addViewer();
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return playerIn.getDistanceSq(tile.getPos()) <= 64;
    }

    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);
        // Notify tile that GUI is closed - reduces sync frequency
        tile.removeViewer();
    }

    public TileFluidTank getTile() {
        return tile;
    }
}
