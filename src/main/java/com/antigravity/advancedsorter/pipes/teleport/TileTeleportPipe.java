package com.antigravity.advancedsorter.pipes.teleport;

import com.antigravity.advancedsorter.pipes.PipeTier;
import com.antigravity.advancedsorter.pipes.TileItemPipe;
import com.antigravity.advancedsorter.pipes.TravellingItem;
import com.antigravity.advancedsorter.network.PacketTeleportInfo;
import com.antigravity.advancedsorter.util.TeleportRegistry;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import java.util.ArrayList;
import java.util.List;

public class TileTeleportPipe extends TileItemPipe {

    private int frequency = 0;
    private TeleportMode mode = TeleportMode.BOTH;

    public TileTeleportPipe() {
        super(PipeTier.DIAMOND); // Teleport pipes are the fastest tier
        this.speed = 2.0f; // Sane speed to ensure center logic triggers
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

    @Override
    public void onLoad() {
        super.onLoad();
        if (!world.isRemote) {
            updateRegistry();
            // Request chunk loading to keep teleporting even when player leaves
            com.antigravity.advancedsorter.util.ChunkLoadingHandler.getInstance()
                    .requestChunkLoading(world, pos);
        }
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        // Don't unregister on chunk unload - we want cross-dimension teleportation to
        // work
        // even when the source chunk is not loaded. Pipes are only unregistered on
        // invalidate().
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (world != null && !world.isRemote) {
            TeleportRegistry.get(world).removePipe(pos, world.provider.getDimension());
            // Release chunk loading
            com.antigravity.advancedsorter.util.ChunkLoadingHandler.getInstance()
                    .releaseChunkLoading(world, pos);
        }
    }

    private void updateRegistry() {
        if (world != null && !world.isRemote) {
            TeleportRegistry.get(world).registerPipe(frequency, world.provider.getDimension(), pos, mode.canSend(),
                    mode.canReceive());
        }
    }

    @Override
    public void update() {
        if (world == null || world.isRemote)
            return;

        if (connectionsDirty) {
            updateConnections();
            connectionsDirty = false;
        }

        if (!travellingItems.isEmpty()) {
            System.out.println("[TELEPORT DEBUG] Processing " + travellingItems.size() + " items at " + pos);
        }

        boolean reachedEndAny = false;
        java.util.Iterator<TravellingItem> iterator = travellingItems.iterator();
        while (iterator.hasNext()) {
            TravellingItem item = iterator.next();

            // Teleport logic at center
            if (item.direction == null && item.progress >= 0.5f) {
                if (!item.teleported && tryTeleport(item)) {
                    iterator.remove();
                    reachedEndAny = true;
                    continue;
                }
                // If teleport failed, fallback to normal pipe logic
                item.direction = chooseOutputDirection(item);
                if (item.direction != null) {
                    System.out.println("[TELEPORT DEBUG] Teleport failed or skipped, chose direction " + item.direction
                            + " at " + pos);
                }
            }

            // Update position
            float oldProgress = item.progress;
            boolean reachedEnd = item.update(speed);
            System.out.println("[TELEPORT DEBUG] Item at " + pos + " progress: " + oldProgress + " -> " + item.progress
                    + ", direction: " + item.direction);

            if (reachedEnd) {
                // Item reached end of pipe
                if (item.direction != null) {
                    boolean transferred = transferToNeighbor(item);

                    if (transferred) {
                        iterator.remove();
                        reachedEndAny = true;
                    } else {
                        // Failed to transfer - bounce back
                        EnumFacing oldDir = item.direction;
                        item.direction = chooseOutputDirectionExcluding(item, oldDir);
                        item.progress = 0.0f;
                        item.source = oldDir.getOpposite();

                        if (item.direction == null) {
                            item.direction = item.source;
                            item.source = oldDir;
                            if (item.direction == null) {
                                item.progress = 0.0f;
                            }
                        }
                    }
                } else {
                    item.progress = 0.0f;
                }
            }
        }

        if (reachedEndAny || !travellingItems.isEmpty()) {
            markDirty();
            sendUpdate();
        }
    }

    public void syncConnectionInfo() {
        if (world != null && !world.isRemote) {
            List<TeleportRegistry.TeleportLocation> all = TeleportRegistry.get(world).getReceivers(frequency);
            List<TeleportRegistry.TeleportLocation> senders = TeleportRegistry.get(world).getSenders(frequency);

            List<TeleportRegistry.TeleportLocation> combined = new ArrayList<>(all);
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
            // Send to all players near this pipe
            for (net.minecraft.entity.player.EntityPlayer player : world.playerEntities) {
                if (player instanceof EntityPlayerMP) {
                    if (player.getDistanceSq(pos) < 1024) { // 32 blocks
                        com.antigravity.advancedsorter.AdvancedSorterMod.network.sendTo(packet,
                                (EntityPlayerMP) player);
                    }
                }
            }
        }
    }

    private boolean tryTeleport(TravellingItem item) {
        if (!mode.canSend()) {
            return false;
        }

        TeleportRegistry registry = TeleportRegistry.get(world);
        List<TeleportRegistry.TeleportLocation> receivers = registry.getReceivers(frequency);

        if (receivers.isEmpty()) {
            return false;
        }

        for (TeleportRegistry.TeleportLocation loc : receivers) {
            if (loc.dimension == world.provider.getDimension() && loc.pos.equals(pos)) {
                continue; // Skip self
            }

            // Get target world, initializing if necessary
            WorldServer targetWorld = DimensionManager.getWorld(loc.dimension);
            if (targetWorld == null) {
                try {
                    DimensionManager.initDimension(loc.dimension);
                    targetWorld = DimensionManager.getWorld(loc.dimension);
                } catch (Exception e) {
                    System.err.println(
                            "[TELEPORT ERROR] Failed to initialize dimension " + loc.dimension + ": " + e.getMessage());
                    continue;
                }
            }

            if (targetWorld != null) {
                // Ensure chunk is loaded and stays loaded for this tick
                if (!targetWorld.isBlockLoaded(loc.pos)) {
                    targetWorld.getChunkProvider().loadChunk(loc.pos.getX() >> 4, loc.pos.getZ() >> 4);
                }

                TileEntity te = targetWorld.getTileEntity(loc.pos);
                if (te instanceof TileTeleportPipe) {
                    TileTeleportPipe targetPipe = (TileTeleportPipe) te;
                    if (targetPipe.getFrequency() != frequency) {
                        continue;
                    }
                    // Receive item at center
                    targetPipe.receiveItem(item.stack, null, true);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected EnumFacing chooseOutputDirection(TravellingItem item) {
        // Fallback to normal pipe logic
        return super.chooseOutputDirection(item);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        frequency = compound.getInteger("Frequency");
        mode = TeleportMode.values()[compound.getInteger("TeleportMode") % TeleportMode.values().length];
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("Frequency", frequency);
        compound.setInteger("TeleportMode", mode.ordinal());
        return compound;
    }

    public enum TeleportMode {
        SEND, RECEIVE, BOTH;

        public boolean canSend() {
            return this == SEND || this == BOTH;
        }

        public boolean canReceive() {
            return this == RECEIVE || this == BOTH;
        }
    }
}
