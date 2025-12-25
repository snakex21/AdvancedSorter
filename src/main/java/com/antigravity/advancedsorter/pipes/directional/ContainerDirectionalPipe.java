package com.antigravity.advancedsorter.pipes.directional;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.EnumFacing;

/**
 * Container for Directional Pipe GUI.
 * Handles button clicks for side mode cycling.
 */
public class ContainerDirectionalPipe extends Container {

    private final TileDirectionalPipe tile;

    public ContainerDirectionalPipe(EntityPlayer player, TileDirectionalPipe tile) {
        this.tile = tile;
    }

    public TileDirectionalPipe getTile() {
        return tile;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return tile != null && !tile.isInvalid();
    }

    /**
     * Handle button click from GUI - cycle the mode for a face.
     */
    public void cycleFaceMode(int faceIndex) {
        if (faceIndex >= 0 && faceIndex < EnumFacing.VALUES.length) {
            EnumFacing face = EnumFacing.VALUES[faceIndex];
            tile.cycleMode(face);
        }
    }
}
