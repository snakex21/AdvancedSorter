package com.antigravity.advancedsorter.pipes.fluid;

import net.minecraft.world.World;

public interface IFluidSyncable {
    void requestClientSync();

    void markSyncableDirty();

    World getSyncableWorld();

    /**
     * Returns true if any player has the GUI open for this syncable.
     * Used for smart sync - frequent updates when GUI open, rare when closed.
     */
    default boolean hasViewers() {
        return false;
    }
}
