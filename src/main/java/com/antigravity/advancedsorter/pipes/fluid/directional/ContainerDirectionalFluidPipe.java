package com.antigravity.advancedsorter.pipes.fluid.directional;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;

/**
 * Container for Directional Fluid Pipe GUI.
 */
public class ContainerDirectionalFluidPipe extends Container {

    private final TileDirectionalFluidPipe tile;

    public ContainerDirectionalFluidPipe(InventoryPlayer playerInv, TileDirectionalFluidPipe tile) {
        this.tile = tile;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    public TileDirectionalFluidPipe getTile() {
        return tile;
    }
}
