package com.antigravity.advancedsorter.pipes;

/**
 * Enum representing the different tiers of pipes.
 * Each tier has different speed (for transport) and throughput (for
 * extraction).
 */
public enum PipeTier {
    STONE("stone", 20.0f, 1), // Slow: 20 ticks/block, 1 item/tick
    IRON("iron", 10.0f, 8), // Medium: 10 ticks/block, 8 items/tick
    GOLD("gold", 5.0f, 32), // Fast: 5 ticks/block, 32 items/tick
    DIAMOND("diamond", 2.0f, 64); // Ultra: 2 ticks/block, 64 items/tick (full stack)

    private final String name;
    private final float speed; // Ticks per block travel
    private final int extractAmount; // Items per extraction tick

    PipeTier(String name, float speed, int extractAmount) {
        this.name = name;
        this.speed = speed;
        this.extractAmount = extractAmount;
    }

    public String getName() {
        return name;
    }

    public float getSpeed() {
        return speed;
    }

    public int getExtractAmount() {
        return extractAmount;
    }
}
