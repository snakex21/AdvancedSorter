package com.antigravity.advancedsorter.pipes.fluid;

/**
 * Enum representing the different tiers of fluid pipes.
 * Each tier has different transfer rate in mB/tick.
 */
public enum FluidPipeTier {
    STONE("stone", 100), // 100 mB/tick
    IRON("iron", 250), // 250 mB/tick
    GOLD("gold", 500), // 500 mB/tick
    DIAMOND("diamond", 1000); // 1000 mB/tick (1 bucket/tick)

    private final String name;
    private final int transferRate; // mB per tick

    FluidPipeTier(String name, int transferRate) {
        this.name = name;
        this.transferRate = transferRate;
    }

    public String getName() {
        return name;
    }

    public int getTransferRate() {
        return transferRate;
    }

    public int getTankCapacity() {
        return transferRate * 10; // Tank holds 10 ticks worth
    }
}
