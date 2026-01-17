package com.antigravity.advancedsorter.pipes.fluid;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;

import javax.annotation.Nullable;

/**
 * A FluidTank that notifies its parent TileFluidPipe when fluid levels change.
 * Uses smart sync: frequent updates when GUI is open, rare updates otherwise.
 */
public class SyncingFluidTank extends FluidTank {

    private IFluidSyncable syncable;
    private int lastAmount = 0;

    // Threshold for sync when GUI is closed (5 buckets)
    private static final int SYNC_THRESHOLD_NO_VIEWERS = 5000;
    // Threshold for sync when GUI is open (any change)
    private static final int SYNC_THRESHOLD_WITH_VIEWERS = 1;

    public SyncingFluidTank(int capacity, IFluidSyncable syncable) {
        super(capacity);
        this.syncable = syncable;
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        int result = super.fill(resource, doFill);
        if (doFill && result > 0) {
            checkForSync();
        }
        return result;
    }

    @Override
    @Nullable
    public FluidStack drain(int maxDrain, boolean doDrain) {
        FluidStack result = super.drain(maxDrain, doDrain);
        if (doDrain && result != null && result.amount > 0) {
            checkForSync();
        }
        return result;
    }

    @Override
    @Nullable
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        FluidStack result = super.drain(resource, doDrain);
        if (doDrain && result != null && result.amount > 0) {
            checkForSync();
        }
        return result;
    }

    private void checkForSync() {
        if (syncable == null || syncable.getSyncableWorld() == null || syncable.getSyncableWorld().isRemote) {
            return;
        }

        int currentAmount = getFluidAmount();
        if (currentAmount == lastAmount) {
            return;
        }

        boolean wasEmpty = lastAmount == 0;
        boolean isEmpty = currentAmount == 0;
        boolean wasFull = lastAmount == getCapacity();
        boolean isFull = currentAmount == getCapacity();
        int diff = Math.abs(currentAmount - lastAmount);

        // Determine sync threshold based on whether anyone is viewing the GUI
        int threshold = syncable.hasViewers() ? SYNC_THRESHOLD_WITH_VIEWERS : SYNC_THRESHOLD_NO_VIEWERS;

        // Always sync on empty/full transitions, otherwise use threshold
        if (wasEmpty || isEmpty || wasFull || isFull || diff >= threshold) {
            lastAmount = currentAmount;
            syncable.markSyncableDirty();
            syncable.requestClientSync();
        }
    }
}
