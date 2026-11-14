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

    // OPTYMALIZACJA: Cache handlerów sąsiadów
    protected final java.util.Map<EnumFacing, IFluidHandler> neighborCache = new java.util.EnumMap<>(EnumFacing.class);

    // OPTYMALIZACJA: Sleep timer dla dystrybucji jeśli nie udało się przesłać
    protected int sleepTimer = 0;

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
            sleepTimer = 0; // Reset sleep on topology change
        }

        // Handle sleep
        if (sleepTimer > 0) {
            sleepTimer--;
        } else {
            // Transfer fluid to neighbors
            if (tank.getFluidAmount() > 0) {
                if (!distributeFluid()) {
                    // If failed to distribute anything, sleep briefly to avoid lag
                    sleepTimer = 10;
                }
            } else {
                // Empty tank, sleep longer
                sleepTimer = 20;
            }
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
        // Save old state to check for changes
        Set<EnumFacing> oldConnections = EnumSet.copyOf(connections);

        connections.clear();
        neighborCache.clear();

        for (EnumFacing face : EnumFacing.VALUES) {
            if (blockedConnections.contains(face))
                continue;

            TileEntity neighbor = world.getTileEntity(pos.offset(face));

            if (canConnectToNeighbor(face, neighbor)) {
                // Also check if neighbor has blocked us
                if (neighbor instanceof TileFluidPipe) {
                    TileFluidPipe neighborPipe = (TileFluidPipe) neighbor;
                    if (neighborPipe.blockedConnections.contains(face.getOpposite())) {
                        continue; // Neighbor blocked connection to us
                    }
                }
                connections.add(face);

                // Cache handler
                if (neighbor != null) {
                    IFluidHandler handler = neighbor.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face.getOpposite());
                    if (handler != null) {
                        neighborCache.put(face, handler);
                    }
                }
            }
        }

        // OPTYMALIZACJA: Tylko wysyłaj update jeśli topologia faktycznie się zmieniła
        if (!connections.equals(oldConnections)) {
            markDirty();
            sendUpdate();
        }
    }

    private boolean canConnectToNeighbor(EnumFacing face, TileEntity neighbor) {
        if (neighbor == null)
            return false;

        // Connect to other fluid pipes
        if (neighbor instanceof TileFluidPipe)
            return true;

        // Connect to any IFluidHandler (tanks, machines, etc.)
        return neighbor.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face.getOpposite());
    }

    protected boolean canConnectTo(EnumFacing face) {
        TileEntity neighbor = world.getTileEntity(pos.offset(face));
        return canConnectToNeighbor(face, neighbor);
    }

    // OPTYMALIZACJA: Kursor Round-Robin dla dystrybucji płynów
    private int distributionCursor = 0;

    protected boolean distributeFluid() {
        if (connections.isEmpty())
            return false;

        int totalToTransfer = Math.min(tank.getFluidAmount(), tier.getTransferRate());
        if (totalToTransfer <= 0)
            return false;

        // Konwersja na listę dla dostępu po indeksie
        // Cache this list usually, but connections change rarely
        java.util.List<EnumFacing> faces = new java.util.ArrayList<>(connections);
        if (faces.isEmpty()) return false;

        boolean transferred = false;
        int startFace = distributionCursor % faces.size();
        int facesChecked = 0;

        while (facesChecked < faces.size() && totalToTransfer > 0) {
            int idx = (startFace + facesChecked) % faces.size();
            EnumFacing face = faces.get(idx);

            // OPTYMALIZACJA: Użyj cache
            IFluidHandler handler = neighborCache.get(face);

            // Fallback
            if (handler == null) {
                 TileEntity neighbor = world.getTileEntity(pos.offset(face));
                 if (neighbor != null) {
                     handler = neighbor.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face.getOpposite());
                 }
            }

            if (handler != null) {
                // Próbuj wysłać wszystko co możemy w tym ticku
                FluidStack fluidToOffer = tank.drain(totalToTransfer, false);
                if (fluidToOffer != null) {
                    int filled = handler.fill(fluidToOffer, true);
                    if (filled > 0) {
                        tank.drain(filled, true);
                        totalToTransfer -= filled;
                        transferred = true;
                    }
                }
            }
            facesChecked++;
        }

        // Aktualizuj kursor - zacznij od następnego w kolejnym ticku
        distributionCursor = (startFace + 1) % faces.size();

        if (transferred) {
            markDirty();
            requestClientSync();
        }
        return transferred;
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

        markConnectionsDirty(); // Ważne: Odśwież cache po zmianie ręcznej
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
