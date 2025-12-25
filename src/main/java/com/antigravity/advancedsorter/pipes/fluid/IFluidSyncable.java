package com.antigravity.advancedsorter.pipes.fluid;

import net.minecraft.world.World;

public interface IFluidSyncable {
    void requestClientSync();

    void markSyncableDirty();

    World getSyncableWorld();
}
