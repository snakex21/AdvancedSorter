package com.antigravity.advancedsorter.pipes.fluid.directional;

import com.antigravity.advancedsorter.pipes.fluid.FluidPipeTier;
import com.antigravity.advancedsorter.pipes.fluid.TileFluidPipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Directional Fluid Pipe - controls fluid flow direction and can filter fluids.
 */
public class TileDirectionalFluidPipe extends TileFluidPipe {

    public enum SideMode {
        DISABLED(0),
        INPUT(1),
        OUTPUT(2);

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

    private final Map<EnumFacing, SideMode> sideModes = new EnumMap<>(EnumFacing.class);
    private final Set<EnumFacing> manuallyConfigured = EnumSet.noneOf(EnumFacing.class);

    // Fluid filtering
    private final List<String> allowedFluids = new ArrayList<>();
    private boolean isWhitelist = true; // true = only allow listed fluids, false = block listed fluids

    public TileDirectionalFluidPipe() {
        super(FluidPipeTier.DIAMOND); // Directional pipes are high tier
        for (EnumFacing face : EnumFacing.VALUES) {
            sideModes.put(face, SideMode.DISABLED);
        }
    }

    public SideMode getSideMode(EnumFacing face) {
        return sideModes.getOrDefault(face, SideMode.DISABLED);
    }

    public void setSideMode(EnumFacing face, SideMode mode) {
        sideModes.put(face, mode);
        manuallyConfigured.add(face);
        markDirty();
        sendUpdate();
    }

    public void cycleMode(EnumFacing face) {
        SideMode current = getSideMode(face);
        setSideMode(face, current.next());
    }

    public boolean isManuallyConfigured(EnumFacing face) {
        return manuallyConfigured.contains(face);
    }

    // ========== Fluid Filtering ==========

    public List<String> getAllowedFluids() {
        return allowedFluids;
    }

    public boolean isWhitelist() {
        return isWhitelist;
    }

    public void setWhitelist(boolean whitelist) {
        this.isWhitelist = whitelist;
        markDirty();
        sendUpdate();
    }

    public void addFluidFilter(String fluidName) {
        if (!allowedFluids.contains(fluidName)) {
            allowedFluids.add(fluidName);
            markDirty();
            sendUpdate();
        }
    }

    public void removeFluidFilter(String fluidName) {
        allowedFluids.remove(fluidName);
        markDirty();
        sendUpdate();
    }

    public void clearFilters() {
        allowedFluids.clear();
        markDirty();
        sendUpdate();
    }

    public boolean isFluidAllowed(FluidStack stack) {
        if (stack == null || stack.getFluid() == null)
            return false;
        if (allowedFluids.isEmpty())
            return true; // No filter = allow all

        String fluidName = stack.getFluid().getName();
        boolean inList = allowedFluids.contains(fluidName);

        return isWhitelist ? inList : !inList;
    }

    // ========== Transfer Logic ==========

    @Override
    protected void distributeFluid() {
        if (tank.getFluidAmount() <= 0)
            return;

        // Check if current fluid is allowed
        if (!isFluidAllowed(tank.getFluid())) {
            return; // Don't transfer blocked fluids
        }

        // Only distribute to OUTPUT sides
        List<EnumFacing> outputSides = new ArrayList<>();
        for (EnumFacing face : connections) {
            if (getSideMode(face) == SideMode.OUTPUT) {
                outputSides.add(face);
            }
        }

        if (outputSides.isEmpty())
            return;

        int amountPerSide = Math.min(tank.getFluidAmount(), tier.getTransferRate()) / outputSides.size();
        if (amountPerSide <= 0)
            return;

        for (EnumFacing face : outputSides) {
            net.minecraft.tileentity.TileEntity neighbor = world.getTileEntity(pos.offset(face));
            if (neighbor == null)
                continue;

            net.minecraftforge.fluids.capability.IFluidHandler handler = neighbor.getCapability(
                    net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY,
                    face.getOpposite());

            if (handler != null) {
                FluidStack toTransfer = tank.drain(amountPerSide, false);
                if (toTransfer != null && toTransfer.amount > 0) {
                    int filled = handler.fill(toTransfer, true);
                    if (filled > 0) {
                        tank.drain(filled, true);
                        markDirty();
                    }
                }
            }
        }
    }

    @Override
    protected void updateConnections() {
        super.updateConnections();
        autoAssignModes();
    }

    private void autoAssignModes() {
        boolean foundFirstInput = false;

        for (EnumFacing face : EnumFacing.VALUES) {
            if (manuallyConfigured.contains(face)) {
                if (getSideMode(face) == SideMode.INPUT) {
                    foundFirstInput = true;
                }
                continue;
            }

            if (canConnectTo(face)) {
                if (!foundFirstInput) {
                    sideModes.put(face, SideMode.INPUT);
                    foundFirstInput = true;
                } else {
                    sideModes.put(face, SideMode.OUTPUT);
                }
            } else {
                sideModes.put(face, SideMode.DISABLED);
            }
        }
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

        isWhitelist = compound.getBoolean("IsWhitelist");

        allowedFluids.clear();
        if (compound.hasKey("AllowedFluids")) {
            NBTTagList fluidList = compound.getTagList("AllowedFluids", Constants.NBT.TAG_STRING);
            for (int i = 0; i < fluidList.tagCount(); i++) {
                allowedFluids.add(fluidList.getStringTagAt(i));
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

        compound.setBoolean("IsWhitelist", isWhitelist);

        NBTTagList fluidList = new NBTTagList();
        for (String fluid : allowedFluids) {
            fluidList.appendTag(new NBTTagString(fluid));
        }
        compound.setTag("AllowedFluids", fluidList);

        return compound;
    }
}
