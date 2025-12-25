package com.antigravity.advancedsorter.pipes.fluid;

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

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.util.math.BlockPos;

/**
 * Base tile entity for fluid transport pipes.
 * Handles fluid storage and transfer to neighbors.
 */
public class TileFluidPipe extends TileEntity implements ITickable, IFluidSyncable {

    protected final FluidPipeTier tier;
    protected final SyncingFluidTank tank;
    protected final Set<EnumFacing> connections = EnumSet.noneOf(EnumFacing.class);
    protected final Set<EnumFacing> blockedConnections = EnumSet.noneOf(EnumFacing.class);
    protected boolean connectionsDirty = true;
    protected boolean syncRequested = false;
    protected int syncCooldown = 0;

    public TileFluidPipe() {
        this(FluidPipeTier.IRON);
    }

    public TileFluidPipe(FluidPipeTier tier) {
        this.tier = tier;
        this.tank = new SyncingFluidTank(tier.getTankCapacity(), this);
    }

    @Override
    public void update() {
        if (world == null || world.isRemote)
            return;

        if (connectionsDirty) {
            updateConnections();
            connectionsDirty = false;
        }

        // Transfer fluid to neighbors
        if (tank.getFluidAmount() > 0) {
            distributeFluid();
        }

        // Handle sync request from tank with rate limiting (every 10 ticks)
        if (syncCooldown > 0) {
            syncCooldown--;
        }
        if (syncRequested && syncCooldown <= 0) {
            syncRequested = false;
            syncCooldown = 10; // Wait 10 ticks before next sync
            sendUpdate();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (world != null && !world.isRemote) {
            // Update our connections
            updateConnections();

            // Notify all neighbor pipes to update their connections
            for (EnumFacing face : EnumFacing.VALUES) {
                BlockPos neighborPos = pos.offset(face);
                TileEntity neighbor = world.getTileEntity(neighborPos);
                if (neighbor instanceof TileFluidPipe) {
                    ((TileFluidPipe) neighbor).updateConnections();
                }
            }
        }
    }

    protected void updateConnections() {
        connections.clear();
        for (EnumFacing face : EnumFacing.VALUES) {
            if (blockedConnections.contains(face))
                continue;

            if (canConnectTo(face)) {
                // Also check if neighbor has blocked us
                BlockPos neighborPos = pos.offset(face);
                TileEntity neighbor = world.getTileEntity(neighborPos);
                if (neighbor instanceof TileFluidPipe) {
                    TileFluidPipe neighborPipe = (TileFluidPipe) neighbor;
                    if (neighborPipe.blockedConnections.contains(face.getOpposite())) {
                        continue; // Neighbor blocked connection to us
                    }
                }
                connections.add(face);
            }
        }
        markDirty();
        sendUpdate();
    }

    protected boolean canConnectTo(EnumFacing face) {
        TileEntity neighbor = world.getTileEntity(pos.offset(face));
        if (neighbor == null)
            return false;

        // Connect to other fluid pipes
        if (neighbor instanceof TileFluidPipe)
            return true;

        // Connect to any IFluidHandler (tanks, machines, etc.)
        return neighbor.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face.getOpposite());
    }

    protected void distributeFluid() {
        if (connections.isEmpty())
            return;

        int amountPerSide = Math.min(tank.getFluidAmount(), tier.getTransferRate()) / connections.size();
        if (amountPerSide <= 0)
            return;

        boolean transferred = false;
        for (EnumFacing face : connections) {
            TileEntity neighbor = world.getTileEntity(pos.offset(face));
            if (neighbor == null)
                continue;

            IFluidHandler handler = neighbor.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY,
                    face.getOpposite());
            if (handler != null) {
                FluidStack toTransfer = tank.drain(amountPerSide, false);
                if (toTransfer != null && toTransfer.amount > 0) {
                    int filled = handler.fill(toTransfer, true);
                    if (filled > 0) {
                        tank.drain(filled, true);
                        transferred = true;
                    }
                }
            }
        }
        if (transferred) {
            markDirty();
            sendUpdate(); // Sync fluid level to client for rendering
        }
    }

    public void markConnectionsDirty() {
        this.connectionsDirty = true;
    }

    /**
     * Toggle connection on a face (wrench action).
     * Disconnecting blocks the connection on BOTH sides.
     * Reconnecting unblocks and re-establishes connection.
     */
    public void toggleConnection(EnumFacing face) {
        BlockPos neighborPos = pos.offset(face);
        TileEntity neighbor = world.getTileEntity(neighborPos);
        TileFluidPipe neighborPipe = (neighbor instanceof TileFluidPipe) ? (TileFluidPipe) neighbor : null;

        if (connections.contains(face)) {
            // Currently connected - DISCONNECT (block both sides)
            connections.remove(face);
            blockedConnections.add(face);

            // Also block on neighbor side
            if (neighborPipe != null) {
                neighborPipe.connections.remove(face.getOpposite());
                neighborPipe.blockedConnections.add(face.getOpposite());
                neighborPipe.markDirty();
                neighborPipe.sendUpdate();
            }
        } else if (blockedConnections.contains(face)) {
            // Currently blocked - UNBLOCK and reconnect if possible
            blockedConnections.remove(face);

            // Also unblock on neighbor side
            if (neighborPipe != null) {
                neighborPipe.blockedConnections.remove(face.getOpposite());
                neighborPipe.markDirty();
            }

            // Try to reconnect
            if (canConnectTo(face)) {
                connections.add(face);
                if (neighborPipe != null) {
                    neighborPipe.connections.add(face.getOpposite());
                    neighborPipe.sendUpdate();
                }
            }
        }

        markDirty();
        sendUpdate();
    }

    public Set<EnumFacing> getConnections() {
        return connections;
    }

    public SyncingFluidTank getTank() {
        return tank;
    }

    public FluidPipeTier getTier() {
        return tier;
    }

    protected void sendUpdate() {
        if (world != null && !world.isRemote) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
    }

    /**
     * Called by SyncingFluidTank when fluid levels change significantly.
     */
    public void requestClientSync() {
        this.syncRequested = true;
    }

    @Override
    public net.minecraft.world.World getSyncableWorld() {
        return getWorld();
    }

    @Override
    public void markSyncableDirty() {
        markDirty();
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return (T) tank;
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        tank.readFromNBT(compound.getCompoundTag("Tank"));

        connections.clear();
        int connMask = compound.getInteger("Connections");
        for (EnumFacing face : EnumFacing.VALUES) {
            if ((connMask & (1 << face.ordinal())) != 0) {
                connections.add(face);
            }
        }

        blockedConnections.clear();
        int blockedMask = compound.getInteger("BlockedConnections");
        for (EnumFacing face : EnumFacing.VALUES) {
            if ((blockedMask & (1 << face.ordinal())) != 0) {
                blockedConnections.add(face);
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Tank", tank.writeToNBT(new NBTTagCompound()));

        int connMask = 0;
        for (EnumFacing face : connections) {
            connMask |= (1 << face.ordinal());
        }
        compound.setInteger("Connections", connMask);

        int blockedMask = 0;
        for (EnumFacing face : blockedConnections) {
            blockedMask |= (1 << face.ordinal());
        }
        compound.setInteger("BlockedConnections", blockedMask);

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
        // Force client to re-render this block
        if (world != null && world.isRemote) {
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
    }
}
