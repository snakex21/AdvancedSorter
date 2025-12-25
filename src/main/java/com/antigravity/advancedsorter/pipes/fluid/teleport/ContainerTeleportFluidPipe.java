package com.antigravity.advancedsorter.pipes.fluid.teleport;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;

/**
 * Container for Teleport Fluid Pipe GUI.
 */
public class ContainerTeleportFluidPipe extends Container {

    private final TileTeleportFluidPipe tile;

    public ContainerTeleportFluidPipe(InventoryPlayer playerInv, TileTeleportFluidPipe tile) {
        this.tile = tile;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    public TileTeleportFluidPipe getTile() {
        return tile;
    }
}
