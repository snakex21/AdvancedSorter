package com.antigravity.advancedsorter.pump;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

/**
 * Container for pump controller GUI.
 */
public class ContainerPumpController extends Container {

    private final TilePumpController tile;

    public ContainerPumpController(TilePumpController tile) {
        this.tile = tile;
    }

    public TilePumpController getTile() {
        return tile;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return tile != null && !tile.isInvalid() && playerIn.getDistanceSq(tile.getPos().add(0.5, 0.5, 0.5)) <= 64.0D;
    }
}
