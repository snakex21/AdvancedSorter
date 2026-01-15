package com.antigravity.advancedsorter.pipes.directional;

import com.antigravity.advancedsorter.pipes.TileItemPipe;
import com.antigravity.advancedsorter.pipes.TravellingItem;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Directional Pipe - forces items to flow in specific directions.
 * Auto-logic: First connection = INPUT, subsequent = OUTPUT.
 * Manual override disables auto-assign for that side.
 */
public class TileDirectionalPipe extends TileItemPipe {

    public enum SideMode {
        DISABLED(0), // Not connected
        INPUT(1), // Accepts items from this side
        OUTPUT(2); // Sends items to this side

        private final int id;

        SideMode(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static SideMode fromId(int id) {
            for (SideMode mode : values()) {
                if (mode.id == id)
                    return mode;
            }
            return DISABLED;
        }

        public SideMode next() {
            switch (this) {
                case DISABLED:
                    return INPUT;
                case INPUT:
                    return OUTPUT;
                case OUTPUT:
                    return DISABLED;
                default:
                    return DISABLED;
            }
        }
    }

    // Configuration for each side
    private final Map<EnumFacing, SideMode> sideModes = new EnumMap<>(EnumFacing.class);

    // Track which sides were manually configured (disables auto-assign)
    private final Set<EnumFacing> manuallyConfigured = EnumSet.noneOf(EnumFacing.class);

    // Track if we have any INPUT side assigned (for auto-logic)
    private boolean hasAutoInput = false;

    public TileDirectionalPipe() {
        // Default: all sides disabled
        for (EnumFacing face : EnumFacing.VALUES) {
            sideModes.put(face, SideMode.DISABLED);
        }
    }

    /**
     * Get the mode for a specific side.
     */
    public SideMode getSideMode(EnumFacing face) {
        return sideModes.getOrDefault(face, SideMode.DISABLED);
    }

    /**
     * Check if side is connected (has something to connect to).
     */
    public boolean isSideConnected(EnumFacing face) {
        return connections.contains(face);
    }

    /**
     * Set the mode for a specific side (manual configuration).
     */
    public void setSideMode(EnumFacing face, SideMode mode) {
        sideModes.put(face, mode);
        manuallyConfigured.add(face); // Mark as manually configured
        markDirty();
        sendUpdate();
    }

    /**
     * Cycle through modes for a side (used by GUI - marks as manual).
     */
    public void cycleMode(EnumFacing face) {
        SideMode current = getSideMode(face);
        setSideMode(face, current.next());
    }

    /**
     * Check if side was manually configured.
     */
    public boolean isManuallyConfigured(EnumFacing face) {
        return manuallyConfigured.contains(face);
    }

    /**
     * Reset to auto mode for a side.
     */
    public void resetToAuto(EnumFacing face) {
        manuallyConfigured.remove(face);
        autoAssignModes();
        markDirty();
        sendUpdate();
    }

    /**
     * Auto-assign modes based on connections.
     * First connection = INPUT, subsequent = OUTPUT.
     */
    private void autoAssignModes() {
        boolean foundFirstInput = false;

        for (EnumFacing face : EnumFacing.VALUES) {
            // Skip manually configured sides
            if (manuallyConfigured.contains(face)) {
                if (getSideMode(face) == SideMode.INPUT) {
                    foundFirstInput = true;
                }
                continue;
            }

            // Check if there's something to connect to
            if (super.canConnectTo(face)) {
                if (!foundFirstInput) {
                    // First connection = INPUT
                    sideModes.put(face, SideMode.INPUT);
                    foundFirstInput = true;
                } else {
                    // Subsequent connections = OUTPUT
                    sideModes.put(face, SideMode.OUTPUT);
                }
            } else {
                sideModes.put(face, SideMode.DISABLED);
            }
        }

        hasAutoInput = foundFirstInput;
    }

    @Override
    public void updateConnections() {
        super.updateConnections();
        // Re-run auto-assign when connections change
        autoAssignModes();
    }

    @Override
    protected boolean canConnectTo(EnumFacing face) {
        // For connection checking, use parent logic
        // Mode filtering happens in chooseOutputDirection and receiveItem
        return super.canConnectTo(face);
    }

    @Override
    protected EnumFacing chooseOutputDirection(TravellingItem item) {
        // Only consider OUTPUT sides
        List<EnumFacing> outputSides = new ArrayList<>();

        for (EnumFacing face : connections) {
            if (face == item.source)
                continue; // Don't go back

            SideMode mode = getSideMode(face);
            if (mode == SideMode.OUTPUT && canOutputTo(face)) {
                outputSides.add(face);
            }
        }

        if (outputSides.isEmpty()) {
            return null;
        }

        if (outputSides.size() == 1) {
            return outputSides.get(0);
        }

        // Round-robin among outputs
        roundRobinIndex = roundRobinIndex % outputSides.size();
        EnumFacing selected = outputSides.get(roundRobinIndex);
        roundRobinIndex = (roundRobinIndex + 1) % outputSides.size();
        return selected;
    }

    /**
     * Check if item can enter from this direction.
     * Only INPUT sides accept items.
     */
    @Override
    public boolean canReceiveItem(net.minecraft.item.ItemStack stack, EnumFacing from) {
        SideMode mode = getSideMode(from);
        if (mode != SideMode.INPUT) {
            return false;
        }
        return true;
    }

    @Override
    public void receiveItem(net.minecraft.item.ItemStack stack, EnumFacing from) {
        // Only called after canReceiveItem returns true
        super.receiveItem(stack, from);
    }

    // ========== NBT ==========

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        if (compound.hasKey("SideModes")) {
            int modes = compound.getInteger("SideModes");
            for (EnumFacing face : EnumFacing.VALUES) {
                int shift = face.getIndex() * 2;
                int modeId = (modes >> shift) & 0x3;
                sideModes.put(face, SideMode.fromId(modeId));
            }
        }

        if (compound.hasKey("ManualSides")) {
            int manual = compound.getInteger("ManualSides");
            manuallyConfigured.clear();
            for (EnumFacing face : EnumFacing.VALUES) {
                if ((manual & (1 << face.getIndex())) != 0) {
                    manuallyConfigured.add(face);
                }
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        int modes = 0;
        for (EnumFacing face : EnumFacing.VALUES) {
            int shift = face.getIndex() * 2;
            modes |= (getSideMode(face).getId() & 0x3) << shift;
        }
        compound.setInteger("SideModes", modes);

        int manual = 0;
        for (EnumFacing face : manuallyConfigured) {
            manual |= (1 << face.getIndex());
        }
        compound.setInteger("ManualSides", manual);

        return compound;
    }
}
