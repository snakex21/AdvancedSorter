package com.antigravity.advancedsorter.pipes.fluid.extraction;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

/**
 * Container for extraction fluid pipe GUI.
 */
public class ContainerExtractionFluidPipe extends Container {

    private final TileExtractionFluidPipe tile;

    public ContainerExtractionFluidPipe(TileExtractionFluidPipe tile) {
        this.tile = tile;
    }

    public TileExtractionFluidPipe getTile() {
        return tile;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return !tile.isInvalid();
    }
}
