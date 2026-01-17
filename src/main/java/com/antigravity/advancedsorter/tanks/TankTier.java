package com.antigravity.advancedsorter.tanks;

import net.minecraft.util.IStringSerializable;

/**
 * Tier system for fluid tanks.
 * Similar to Mekanism's tier progression.
 */
public enum TankTier implements IStringSerializable {
    BASIC(0, "basic", 16000, 100, 0x8B8B8B),       // 16 Buckets, Gray
    ADVANCED(1, "advanced", 64000, 400, 0xFF5555),  // 64 Buckets, Red
    ELITE(2, "elite", 256000, 1600, 0x5555FF),      // 256 Buckets, Blue
    ULTIMATE(3, "ultimate", 1024000, 6400, 0x55FF55); // 1024 Buckets, Green

    private final int id;
    private final String name;
    private final int capacity;
    private final int transferRate; // mB per tick
    private final int color;

    TankTier(int id, String name, int capacity, int transferRate, int color) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.transferRate = transferRate;
        this.color = color;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getTransferRate() {
        return transferRate;
    }

    public int getColor() {
        return color;
    }

    public String getUnlocalizedName() {
        return "tile.advancedsorter.fluid_tank_" + name + ".name";
    }

    public static TankTier fromId(int id) {
        for (TankTier tier : values()) {
            if (tier.id == id) {
                return tier;
            }
        }
        return BASIC;
    }

    public static TankTier fromMeta(int meta) {
        if (meta >= 0 && meta < values().length) {
            return values()[meta];
        }
        return BASIC;
    }
}
