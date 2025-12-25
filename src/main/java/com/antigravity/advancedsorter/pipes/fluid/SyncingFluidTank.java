package com.antigravity.advancedsorter.pipes.fluid;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;

import javax.annotation.Nullable;

/**
 * A FluidTank that notifies its parent TileFluidPipe when fluid levels change.
 */
public class SyncingFluidTank extends FluidTank {

    private IFluidSyncable syncable;
    private int lastAmount = 0;

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
        int currentAmount = getFluidAmount();
        // Sync if amount changed significantly (more than 10% or from/to empty)
        if (currentAmount != lastAmount) {
            boolean wasEmpty = lastAmount == 0;
            boolean isEmpty = currentAmount == 0;
            int diff = Math.abs(currentAmount - lastAmount);

            // Sync on empty/fill transitions or every 10% change
            if (wasEmpty || isEmpty || diff > getCapacity() / 10) {
                lastAmount = currentAmount;
                if (syncable != null && syncable.getSyncableWorld() != null && !syncable.getSyncableWorld().isRemote) {
                    syncable.markSyncableDirty();
                    syncable.requestClientSync();
                }
            }
        }
    }
}
