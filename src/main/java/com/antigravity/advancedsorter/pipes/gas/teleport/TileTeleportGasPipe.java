package com.antigravity.advancedsorter.pipes.gas.teleport;

import com.antigravity.advancedsorter.AdvancedSorterMod;
import com.antigravity.advancedsorter.network.PacketTeleportInfo;
import com.antigravity.advancedsorter.util.TeleportRegistry;
import mekanism.api.gas.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Teleport Gas Pipe - transfers gases across dimensions using Mekanism gas API.
 */
public class TileTeleportGasPipe extends TileEntity implements ITickable, IGasHandler {

    @CapabilityInject(IGasHandler.class)
    public static Capability<IGasHandler> GAS_HANDLER_CAPABILITY = null;

    private static final int TANK_CAPACITY = 10000;
    private static final int TRANSFER_RATE = 1000;

    private int frequency = 0;
    private TeleportMode mode = TeleportMode.BOTH;

    private boolean syncRequested = false;
    private int syncCooldown = 0;

    protected final GasTank tank = new GasTank(TANK_CAPACITY) {
        @Override
        public int receive(GasStack stack, boolean doTransfer) {
            int added = super.receive(stack, doTransfer);
            if (doTransfer && added > 0) {
                syncRequested = true;
                markDirty();
            }
            return added;
        }

        @Override
        public GasStack draw(int amount, boolean doTransfer) {
            GasStack drained = super.draw(amount, doTransfer);
            if (doTransfer && drained != null && drained.amount > 0) {
                syncRequested = true;
                markDirty();
            }
            return drained;
        }
    };

    private final Set<EnumFacing> connections = EnumSet.noneOf(EnumFacing.class);
    private final Set<EnumFacing> blockedConnections = EnumSet.noneOf(EnumFacing.class);
    private boolean connectionsDirty = true;

    public enum TeleportMode {
        SEND(true, false),
        RECEIVE(false, true),
        BOTH(true, true);

        private final boolean canSend;
        private final boolean canReceive;

        TeleportMode(boolean canSend, boolean canReceive) {
            this.canSend = canSend;
            this.canReceive = canReceive;
        }

        public boolean canSend() {
            return canSend;
        }

        public boolean canReceive() {
            return canReceive;
        }
    }

    public TileTeleportGasPipe() {
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
        updateRegistry();
        markDirty();
        sendUpdate();
        syncConnectionInfo();
    }

    public TeleportMode getMode() {
        return mode;
    }

    public void setMode(TeleportMode mode) {
        this.mode = mode;
        updateRegistry();
        markDirty();
        sendUpdate();
        syncConnectionInfo();
    }

    public Set<EnumFacing> getConnections() {
        return connections;
    }

    public void checkConnections() {
        connectionsDirty = true;
    }

    public void toggleConnection(EnumFacing side) {
        BlockPos neighborPos = pos.offset(side);
        TileEntity neighbor = world.getTileEntity(neighborPos);
        TileTeleportGasPipe neighborPipe = (neighbor instanceof TileTeleportGasPipe) ? (TileTeleportGasPipe) neighbor : null;

        if (connections.contains(side)) {
            // Currently connected - DISCONNECT (block both sides)
            connections.remove(side);
            blockedConnections.add(side);

            // Also block on neighbor side
            if (neighborPipe != null) {
                neighborPipe.connections.remove(side.getOpposite());
                neighborPipe.blockedConnections.add(side.getOpposite());
                neighborPipe.markDirty();
                neighborPipe.sendUpdate();
            }
        } else if (blockedConnections.contains(side)) {
            // Currently blocked - UNBLOCK and reconnect if possible
            blockedConnections.remove(side);

            // Also unblock on neighbor side
            if (neighborPipe != null) {
                neighborPipe.blockedConnections.remove(side.getOpposite());
                neighborPipe.markDirty();
            }

            // The connections will be re-established in updateConnections() which is called via connectionsDirty
        }

        connectionsDirty = true;
        markDirty();
        sendUpdate();

        // Ensure neighbor also updates its connections next tick
        if (neighborPipe != null) {
            neighborPipe.connectionsDirty = true;
            neighborPipe.sendUpdate();
        }
    }

    private void updateConnections() {
        if (world == null || world.isRemote) return;

        Set<EnumFacing> newConnections = EnumSet.noneOf(EnumFacing.class);

        for (EnumFacing side : EnumFacing.VALUES) {
            if (blockedConnections.contains(side)) continue;

            if (canConnectTo(side)) {
                // Also check if neighbor has blocked us
                BlockPos neighborPos = pos.offset(side);
                TileEntity neighbor = world.getTileEntity(neighborPos);
                if (neighbor instanceof TileTeleportGasPipe) {
                    TileTeleportGasPipe neighborPipe = (TileTeleportGasPipe) neighbor;
                    if (neighborPipe.blockedConnections.contains(side.getOpposite())) {
                        continue; // Neighbor blocked connection to us
                    }
                }

                newConnections.add(side);
            }
        }

        if (!connections.equals(newConnections)) {
            connections.clear();
            connections.addAll(newConnections);
            markDirty();
            sendUpdate();
        }
        connectionsDirty = false;
    }

    private boolean canConnectTo(EnumFacing side) {
        TileEntity tile = world.getTileEntity(pos.offset(side));
        if (tile == null) return false;

        // Connect to other teleport pipes
        if (tile instanceof TileTeleportGasPipe) return true;

        if (tile.hasCapability(GAS_HANDLER_CAPABILITY, side.getOpposite())) {
            return true;
        }
        return tile instanceof IGasHandler;
    }

    public GasTank getTank() {
        return tank;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (world != null && !world.isRemote) {
            updateRegistry();
            // Request chunk loading to keep teleporting even when player leaves
            com.antigravity.advancedsorter.util.ChunkLoadingHandler.getInstance()
                    .requestChunkLoading(world, pos);
            checkConnections();
        }
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        // Don't unregister on chunk unload - we want cross-dimension teleportation to work
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (world != null && !world.isRemote) {
            TeleportRegistry.get(world).removeGasPipe(pos, world.provider.getDimension());
            // Release chunk loading
            com.antigravity.advancedsorter.util.ChunkLoadingHandler.getInstance()
                    .releaseChunkLoading(world, pos);
        }
    }

    private void updateRegistry() {
        if (world != null && !world.isRemote) {
            TeleportRegistry.get(world).registerGasPipe(frequency, world.provider.getDimension(), pos,
                    mode.canSend(), mode.canReceive());
        }
    }

    @Override
    public void update() {
        if (world == null || world.isRemote)
            return;

        // Handle sync request from tank with rate limiting
        if (syncCooldown > 0) {
            syncCooldown--;
        }
        if (syncRequested && syncCooldown <= 0) {
            syncRequested = false;
            syncCooldown = 10; // Wait 10 ticks before next sync
            sendUpdate();
        }

        if (connectionsDirty) {
            updateConnections();
        }

        // Try to teleport gas if we can send
        if (mode.canSend() && tank.getStored() > 0) {
            tryTeleportGas();
        }

        // Try to output gas to neighbors if we can receive (and thus have gas to give)
        if (mode.canReceive() && tank.getStored() > 0) {
            distributeGas();
        }
    }

    private void distributeGas() {
        if (connections.isEmpty()) return;

        for (EnumFacing side : connections) {
            TileEntity tile = world.getTileEntity(pos.offset(side));
            if (tile == null) continue;

            // Check for Gas Handler Capability
            if (tile.hasCapability(GAS_HANDLER_CAPABILITY, side.getOpposite())) {
                IGasHandler handler = tile.getCapability(GAS_HANDLER_CAPABILITY, side.getOpposite());
                if (handler != null && handler.canReceiveGas(side.getOpposite(), tank.getGasType())) {
                    int amountToSend = Math.min(tank.getStored(), TRANSFER_RATE);
                    GasStack toSend = new GasStack(tank.getGasType(), amountToSend);

                    int accepted = handler.receiveGas(side.getOpposite(), toSend, true);
                    if (accepted > 0) {
                        tank.draw(accepted, true);
                        if (tank.getStored() <= 0) break;
                    }
                }
            }
            // Legacy/Direct IGasHandler check (just in case)
            else if (tile instanceof IGasHandler) {
                IGasHandler handler = (IGasHandler) tile;
                if (handler.canReceiveGas(side.getOpposite(), tank.getGasType())) {
                    int amountToSend = Math.min(tank.getStored(), TRANSFER_RATE);
                    GasStack toSend = new GasStack(tank.getGasType(), amountToSend);

                    int accepted = handler.receiveGas(side.getOpposite(), toSend, true);
                    if (accepted > 0) {
                        tank.draw(accepted, true);
                        if (tank.getStored() <= 0) break;
                    }
                }
            }
        }
    }

    private void tryTeleportGas() {
        TeleportRegistry registry = TeleportRegistry.get(world);
        List<TeleportRegistry.TeleportLocation> receivers = registry.getGasReceivers(frequency);

        if (receivers.isEmpty())
            return;

        int amountToSend = Math.min(tank.getStored(), TRANSFER_RATE);

        for (TeleportRegistry.TeleportLocation loc : receivers) {
            if (loc.dimension == world.provider.getDimension() && loc.pos.equals(pos)) {
                continue; // Skip self
            }

            WorldServer targetWorld = DimensionManager.getWorld(loc.dimension);
            if (targetWorld == null) {
                try {
                    DimensionManager.initDimension(loc.dimension);
                    targetWorld = DimensionManager.getWorld(loc.dimension);
                } catch (Exception e) {
                    continue;
                }
            }

            if (targetWorld != null) {
                if (!targetWorld.isBlockLoaded(loc.pos)) {
                    targetWorld.getChunkProvider().loadChunk(loc.pos.getX() >> 4, loc.pos.getZ() >> 4);
                }

                TileEntity te = targetWorld.getTileEntity(loc.pos);
                if (te instanceof TileTeleportGasPipe) {
                    TileTeleportGasPipe targetPipe = (TileTeleportGasPipe) te;
                    if (targetPipe.getFrequency() != frequency)
                        continue;
                    if (!targetPipe.getMode().canReceive())
                        continue;

                    // Transfer gas
                    GasStack toTransfer = tank.draw(amountToSend, false);
                    if (toTransfer != null && toTransfer.amount > 0) {
                        int received = targetPipe.tank.receive(toTransfer, true);
                        if (received > 0) {
                            tank.draw(received, true);
                            targetPipe.markDirty();
                            targetPipe.sendUpdate();
                            return; // Only transfer to one receiver per tick
                        }
                    }
                }
            }
        }
    }

    // ========== IGasHandler Implementation ==========
    // Note: When this pipe is in SEND mode, external machines should INSERT gas into us
    //       When this pipe is in RECEIVE mode, external machines should EXTRACT gas from us

    @Override
    public int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
        // External machines insert gas -> we need to be able to SEND it via teleportation
        if (!mode.canSend()) {
            return 0;
        }
        return tank.receive(stack, doTransfer);
    }

    @Override
    public GasStack drawGas(EnumFacing side, int amount, boolean doTransfer) {
        // External machines extract gas -> we need to have RECEIVED it via teleportation
        if (!mode.canReceive()) {
            return null;
        }
        return tank.draw(amount, doTransfer);
    }

    @Override
    public boolean canReceiveGas(EnumFacing side, Gas type) {
        return mode.canSend() && tank.canReceive(type);
    }

    @Override
    public boolean canDrawGas(EnumFacing side, Gas type) {
        return mode.canReceive() && tank.canDraw(type);
    }

    @Nonnull
    @Override
    public GasTankInfo[] getTankInfo() {
        return new GasTankInfo[]{tank};
    }

    // ========== Capabilities ==========

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing side) {
        if (GAS_HANDLER_CAPABILITY != null && capability == GAS_HANDLER_CAPABILITY) {
            return true;
        }
        return super.hasCapability(capability, side);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing side) {
        if (GAS_HANDLER_CAPABILITY != null && capability == GAS_HANDLER_CAPABILITY) {
            return GAS_HANDLER_CAPABILITY.cast(this);
        }
        return super.getCapability(capability, side);
    }

    // ========== NBT ==========

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        frequency = compound.getInteger("Frequency");
        mode = TeleportMode.values()[compound.getInteger("Mode") % TeleportMode.values().length];
        tank.read(compound.getCompoundTag("GasTank"));

        connections.clear();
        int packedConnections = compound.getInteger("Connections");
        for (EnumFacing side : EnumFacing.VALUES) {
            if ((packedConnections & (1 << side.getIndex())) != 0) {
                connections.add(side);
            }
        }

        blockedConnections.clear();
        int packedBlocked = compound.getInteger("BlockedConnections");
        for (EnumFacing side : EnumFacing.VALUES) {
            if ((packedBlocked & (1 << side.getIndex())) != 0) {
                blockedConnections.add(side);
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("Frequency", frequency);
        compound.setInteger("Mode", mode.ordinal());
        compound.setTag("GasTank", tank.write(new NBTTagCompound()));

        int packedConnections = 0;
        for (EnumFacing side : connections) {
            packedConnections |= (1 << side.getIndex());
        }
        compound.setInteger("Connections", packedConnections);

        int packedBlocked = 0;
        for (EnumFacing side : blockedConnections) {
            packedBlocked |= (1 << side.getIndex());
        }
        compound.setInteger("BlockedConnections", packedBlocked);

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
        if (world != null && world.isRemote) {
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
    }

    protected void sendUpdate() {
        if (world != null && !world.isRemote) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
    }

    public void syncConnectionInfo() {
        if (world != null && !world.isRemote) {
            TeleportRegistry registry = TeleportRegistry.get(world);
            List<TeleportRegistry.TeleportLocation> receivers = registry.getGasReceivers(frequency);
            List<TeleportRegistry.TeleportLocation> senders = registry.getGasSenders(frequency);

            List<TeleportRegistry.TeleportLocation> combined = new ArrayList<>(receivers);
            for (TeleportRegistry.TeleportLocation loc : senders) {
                boolean exists = false;
                for (TeleportRegistry.TeleportLocation existing : combined) {
                    if (existing.pos.equals(loc.pos) && existing.dimension == loc.dimension) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    combined.add(loc);
                }
            }

            PacketTeleportInfo packet = new PacketTeleportInfo(combined);
            for (net.minecraft.entity.player.EntityPlayer player : world.playerEntities) {
                if (player instanceof EntityPlayerMP) {
                    if (player.getDistanceSq(pos) < 1024) {
                        AdvancedSorterMod.network.sendTo(packet, (EntityPlayerMP) player);
                    }
                }
            }
        }
    }
}
