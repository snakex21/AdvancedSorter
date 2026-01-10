package com.antigravity.advancedsorter.pipes.gas.teleport;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;

/**
 * Container for Teleport Gas Pipe GUI.
 */
public class ContainerTeleportGasPipe extends Container {

    private final TileTeleportGasPipe tile;

    public ContainerTeleportGasPipe(InventoryPlayer playerInv, TileTeleportGasPipe tile) {
        this.tile = tile;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    public TileTeleportGasPipe getTile() {
        return tile;
    }
}
