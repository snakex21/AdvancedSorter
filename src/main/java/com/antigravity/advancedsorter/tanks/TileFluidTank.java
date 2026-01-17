package com.antigravity.advancedsorter.tanks;

import com.antigravity.advancedsorter.pipes.fluid.IFluidSyncable;
import com.antigravity.advancedsorter.pipes.fluid.SyncingFluidTank;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

public class TileFluidTank extends TileEntity implements ITickable, IFluidSyncable {

    public enum SideMode {
        DISABLED(0, "X"),
        INPUT(1, "IN"),
        OUTPUT(2, "OUT");

        private final int id;
        private final String label;

        SideMode(int id, String label) {
            this.id = id;
            this.label = label;
        }

        public int getId() {
            return id;
        }

        public String getLabel() {
            return label;
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

    private TankTier tier;
    private SyncingFluidTank tank;
    private final Map<EnumFacing, SideMode> sideModes = new EnumMap<>(EnumFacing.class);

    // Cached sided handlers to avoid creating new objects every getCapability call
    private final Map<EnumFacing, SidedFluidHandler> cachedHandlers = new EnumMap<>(EnumFacing.class);

    private boolean syncRequested = false;
    private int syncCooldown = 0;

    // Cached output count to avoid recalculating every tick
    private int cachedOutputCount = 0;
    private boolean outputCountDirty = true;

    // Optimization: Tick skipping and neighbor caching
    private int tickCounter = 0;
    private final Map<EnumFacing, IFluidHandler> cachedNeighbors = new EnumMap<>(EnumFacing.class);
    private int neighborCacheCooldown = 0;

    // Track viewers for smart sync
    private int viewerCount = 0;

    public TileFluidTank() {
        this(TankTier.BASIC);
    }

    public TileFluidTank(TankTier tier) {
        this.tier = tier;
        this.tank = new SyncingFluidTank(tier.getCapacity(), this);
        for (EnumFacing face : EnumFacing.VALUES) {
            sideModes.put(face, SideMode.INPUT); // Default to INPUT
        }
        rebuildHandlerCache();
    }

    private void rebuildHandlerCache() {
        cachedHandlers.clear();
        cachedOutputCount = 0;
        for (EnumFacing face : EnumFacing.VALUES) {
            SideMode mode = sideModes.get(face);
            if (mode != SideMode.DISABLED) {
                cachedHandlers.put(face, new SidedFluidHandler(mode));
            }
            if (mode == SideMode.OUTPUT) {
                cachedOutputCount++;
            }
        }
        outputCountDirty = false;
    }

    @Override
    public void update() {
        if (world == null || world.isRemote)
            return;

        tickCounter++;

        // Refresh neighbor cache every 100 ticks (5 seconds) to handle block updates
        if (neighborCacheCooldown > 0) {
            neighborCacheCooldown--;
        } else {
            cachedNeighbors.clear();
            neighborCacheCooldown = 100;
        }

        // Tick Skipping: Only push fluid every 5 ticks (4 times per second)
        // This reduces CPU load by 80% for transfer operations
        if (tickCounter % 5 == 0 && cachedOutputCount > 0 && tank.getFluidAmount() > 0) {
            pushFluid();
        }

        // Handle sync request with rate limiting
        if (syncCooldown > 0) {
            syncCooldown--;
        }
        if (syncRequested && syncCooldown <= 0) {
            syncRequested = false;
            syncCooldown = 10;
            sendUpdate();
        }
    }

    private void pushFluid() {
        // Multiply transfer rate by 5 because we run 5x less often
        int totalPush = tier.getTransferRate() * 5;
        int pushPerSide = totalPush / cachedOutputCount;
        if (pushPerSide <= 0) pushPerSide = 1;

        for (EnumFacing face : EnumFacing.VALUES) {
            if (sideModes.get(face) == SideMode.OUTPUT && tank.getFluidAmount() > 0) {

                // Optimization: Use cached neighbor handler
                IFluidHandler handler = cachedNeighbors.get(face);

                // If not in cache, try to find it
                if (handler == null && !cachedNeighbors.containsKey(face)) {
                    TileEntity neighbor = world.getTileEntity(pos.offset(face));
                    if (neighbor != null && neighbor.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face.getOpposite())) {
                        handler = neighbor.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face.getOpposite());
                    }
                    // Cache the result (even if null, to avoid re-querying empty air blocks repeatedly)
                    cachedNeighbors.put(face, handler);
                }

                if (handler != null) {
                    FluidStack toDrain = tank.drain(pushPerSide, false);
                    if (toDrain != null && toDrain.amount > 0) {
                        int filled = handler.fill(toDrain, true);
                        if (filled > 0) {
                            tank.drain(filled, true);
                        } else {
                            // If filling failed completely, maybe the neighbor is full or invalid
                            // We keep it in cache until the 5-second refresh cycle
                        }
                    }
                }
            }
        }
    }

    public TankTier getTier() {
        return tier;
    }

    public SideMode getSideMode(EnumFacing face) {
        return sideModes.getOrDefault(face, SideMode.DISABLED);
    }

    public void setSideMode(EnumFacing face, SideMode mode) {
        sideModes.put(face, mode);
        rebuildHandlerCache();
        markDirty();
        sendUpdate();
    }

    public void cycleMode(EnumFacing face) {
        SideMode current = getSideMode(face);
        setSideMode(face, current.next());
    }

    /**
     * Check if there's a fluid handler (pipe, tank, etc.) connected on the given side.
     */
    public boolean hasFluidConnection(EnumFacing face) {
        if (world == null) return false;
        TileEntity neighbor = world.getTileEntity(pos.offset(face));
        if (neighbor != null) {
            return neighbor.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face.getOpposite());
        }
        return false;
    }

    public SyncingFluidTank getTank() {
        return tank;
    }

    // Viewer tracking for smart sync
    public void addViewer() {
        viewerCount++;
    }

    public void removeViewer() {
        viewerCount = Math.max(0, viewerCount - 1);
    }

    public boolean hasViewers() {
        return viewerCount > 0;
    }

    // For saving/loading fluid when block is picked up
    public void writeToItemNBT(NBTTagCompound compound) {
        compound.setTag("Tank", tank.writeToNBT(new NBTTagCompound()));
    }

    public void readFromItemNBT(NBTTagCompound compound) {
        if (compound.hasKey("Tank")) {
            tank.readFromNBT(compound.getCompoundTag("Tank"));
        }
    }

    // IFluidSyncable implementation
    @Override
    public net.minecraft.world.World getSyncableWorld() {
        return getWorld();
    }

    @Override
    public void markSyncableDirty() {
        markDirty();
    }

    @Override
    public void requestClientSync() {
        this.syncRequested = true;
    }

    private void sendUpdate() {
        if (world != null && !world.isRemote) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    // Capability System
    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            if (facing == null) return true;
            return sideModes.get(facing) != SideMode.DISABLED;
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            if (facing == null) {
                return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(tank);
            }

            SidedFluidHandler handler = cachedHandlers.get(facing);
            if (handler != null) {
                return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(handler);
            }
            return null;
        }
        return super.getCapability(capability, facing);
    }

    // Wrapper for sided access
    private class SidedFluidHandler implements IFluidHandler {
        private final SideMode mode;

        SidedFluidHandler(SideMode mode) {
            this.mode = mode;
        }

        @Override
        public IFluidTankProperties[] getTankProperties() {
            return tank.getTankProperties();
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            if (mode == SideMode.INPUT) {
                return tank.fill(resource, doFill);
            }
            return 0;
        }

        @Nullable
        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            if (mode == SideMode.OUTPUT) {
                return tank.drain(resource, doDrain);
            }
            return null;
        }

        @Nullable
        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            if (mode == SideMode.OUTPUT) {
                return tank.drain(maxDrain, doDrain);
            }
            return null;
        }
    }

    // NBT Handling
    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        if (compound.hasKey("Tier")) {
            tier = TankTier.fromId(compound.getInteger("Tier"));
            // Recreate tank with correct capacity
            FluidStack oldFluid = tank.getFluid();
            tank = new SyncingFluidTank(tier.getCapacity(), this);
            if (oldFluid != null) {
                tank.fill(oldFluid, true);
            }
        }

        tank.readFromNBT(compound.getCompoundTag("Tank"));

        if (compound.hasKey("SideModes")) {
            int modes = compound.getInteger("SideModes");
            for (EnumFacing face : EnumFacing.VALUES) {
                int shift = face.getIndex() * 2;
                int modeId = (modes >> shift) & 0x3;
                sideModes.put(face, SideMode.fromId(modeId));
            }
        }

        rebuildHandlerCache();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        compound.setInteger("Tier", tier.getId());
        compound.setTag("Tank", tank.writeToNBT(new NBTTagCompound()));

        int modes = 0;
        for (EnumFacing face : EnumFacing.VALUES) {
            int shift = face.getIndex() * 2;
            modes |= (getSideMode(face).getId() & 0x3) << shift;
        }
        compound.setInteger("SideModes", modes);

        return compound;
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public net.minecraft.network.play.server.SPacketUpdateTileEntity getUpdatePacket() {
        return new net.minecraft.network.play.server.SPacketUpdateTileEntity(pos, 1, getUpdateTag());
    }

    @Override
    public void onDataPacket(net.minecraft.network.NetworkManager net,
            net.minecraft.network.play.server.SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }
}
