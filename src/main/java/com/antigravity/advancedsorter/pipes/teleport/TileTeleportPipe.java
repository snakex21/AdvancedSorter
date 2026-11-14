package com.antigravity.advancedsorter.pipes.teleport;

import com.antigravity.advancedsorter.pipes.PipeTier;
import com.antigravity.advancedsorter.pipes.TileItemPipe;
import com.antigravity.advancedsorter.pipes.TravellingItem;
import com.antigravity.advancedsorter.network.PacketTeleportInfo;
import com.antigravity.advancedsorter.util.TeleportRegistry;
import com.antigravity.advancedsorter.util.PendingTeleportRegistry;
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
    private int roundRobinIndex = 0;

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
        TeleportMode oldMode = this.mode;
        this.mode = mode;
        updateRegistry();

        // Aktualizuj chunk loading przy zmianie trybu
        if (world != null && !world.isRemote) {
            if (mode.canSend() && !oldMode.canSend()) {
                // Teraz wysyła - załaduj chunk
                com.antigravity.advancedsorter.util.ChunkLoadingHandler.getInstance()
                        .requestChunkLoading(world, pos);
            } else if (!mode.canSend() && oldMode.canSend()) {
                // Już nie wysyła - zwolnij chunk
                com.antigravity.advancedsorter.util.ChunkLoadingHandler.getInstance()
                        .releaseChunkLoading(world, pos);
            }

            // Odbierz oczekujące przedmioty jeśli teraz może odbierać
            if (mode.canReceive() && !oldMode.canReceive()) {
                retrievePendingItems();
            }
        }

        markDirty();
        sendUpdate();
        syncConnectionInfo();
    }

    @Override
    public void validate() {
        super.validate();
        if (world != null && !world.isRemote) {
            updateRegistry();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (world != null && !world.isRemote) {
            updateRegistry();

            // Rury WYSYŁAJĄCE utrzymują swój chunk załadowany (żeby produkcja działała)
            if (mode.canSend()) {
                com.antigravity.advancedsorter.util.ChunkLoadingHandler.getInstance()
                        .requestChunkLoading(world, pos);
            }

            // Odbierz oczekujące przedmioty z kolejki (wysłane gdy chunk był niezaładowany)
            if (mode.canReceive()) {
                retrievePendingItems();
            }
        }
    }

    /**
     * Odbiera przedmioty które były wysłane gdy ten chunk był niezaładowany.
     */
    private void retrievePendingItems() {
        if (world == null || world.isRemote) return;
        if (!mode.canReceive()) return; // Tylko receivery odbierają

        PendingTeleportRegistry pending = PendingTeleportRegistry.get(world);
        java.util.List<net.minecraft.item.ItemStack> items = pending.retrievePendingItems(
                world.provider.getDimension(), pos);

        for (net.minecraft.item.ItemStack stack : items) {
            if (!stack.isEmpty()) {
                receiveItem(stack, null, true);
            }
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
            // Zwolnij chunk loading jeśli rura wysyłała
            if (mode.canSend()) {
                com.antigravity.advancedsorter.util.ChunkLoadingHandler.getInstance()
                        .releaseChunkLoading(world, pos);
            }
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
        if (world == null)
            return;

        // CLIENT SIDE: Interpolation
        if (world.isRemote) {
            for (TravellingItem item : travellingItems) {
                item.update(speed);
            }
            return;
        }

        // SERVER SIDE
        // OPTYMALIZACJA: Idle sleep logic
        if (travellingItems.isEmpty() && !connectionsDirty) {
            if (sleepTimer > 0) {
                sleepTimer--;
                return;
            }
            sleepTimer = 40;
        } else {
            sleepTimer = 0;
        }

        boolean stateChanged = false;

        if (connectionsDirty) {
            updateConnections();
            connectionsDirty = false;
        }

        java.util.Iterator<TravellingItem> iterator = travellingItems.iterator();
        while (iterator.hasNext()) {
            TravellingItem item = iterator.next();

            // Teleport logic at center
            if (item.direction == null && item.progress >= 0.5f) {
                if (!item.teleported && tryTeleport(item)) {
                    iterator.remove();
                    stateChanged = true; // Item teleported away
                    continue;
                }
                // If teleport failed, fallback to normal pipe logic
                item.direction = chooseOutputDirection(item);
                stateChanged = true; // Direction chosen
            }

            // Update position
            boolean reachedEnd = item.update(speed);

            if (reachedEnd) {
                // Item reached end of pipe
                if (item.direction != null) {
                    boolean transferred = transferToNeighbor(item);

                    if (transferred) {
                        iterator.remove();
                        stateChanged = true; // Item transferred
                    } else {
                        // Failed to transfer - bounce back
                        EnumFacing oldDir = item.direction;
                        item.direction = chooseOutputDirectionExcluding(item, oldDir);
                        item.progress = 0.0f;
                        item.source = oldDir.getOpposite();

                        stateChanged = true; // Bounce

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
                    stateChanged = true; // Reset
                }
            }
        }

        // Only sync on state changes
        if (stateChanged) {
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

        int startIdx = (roundRobinIndex + 1) % receivers.size();

        for (int i = 0; i < receivers.size(); i++) {
            int currentIdx = (startIdx + i) % receivers.size();
            TeleportRegistry.TeleportLocation loc = receivers.get(currentIdx);

            if (loc.dimension == world.provider.getDimension() && loc.pos.equals(pos)) {
                continue; // Skip self
            }

            // Pobierz świat docelowy
            WorldServer targetWorld = DimensionManager.getWorld(loc.dimension);

            // Jeśli wymiar nie jest załadowany - kolejkuj przedmiot
            if (targetWorld == null) {
                // Kolejkuj przedmiot - zostanie dostarczony gdy chunk się załaduje
                PendingTeleportRegistry.get(world).queueItem(loc.dimension, loc.pos, item.stack);
                roundRobinIndex = currentIdx;
                return true; // Przedmiot "wysłany" (do kolejki)
            }

            // Jeśli chunk nie jest załadowany - kolejkuj przedmiot
            if (!targetWorld.isBlockLoaded(loc.pos)) {
                // Kolejkuj przedmiot - zostanie dostarczony gdy chunk się załaduje
                PendingTeleportRegistry.get(world).queueItem(loc.dimension, loc.pos, item.stack);
                roundRobinIndex = currentIdx;
                return true; // Przedmiot "wysłany" (do kolejki)
            }

            // Chunk jest załadowany - teleportuj bezpośrednio
            TileEntity te = targetWorld.getTileEntity(loc.pos);
            if (te instanceof TileTeleportPipe) {
                TileTeleportPipe targetPipe = (TileTeleportPipe) te;
                if (targetPipe.getFrequency() != frequency) {
                    continue;
                }
                // Receive item at center
                targetPipe.receiveItem(item.stack, null, true);
                roundRobinIndex = currentIdx;
                return true;
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
        roundRobinIndex = compound.getInteger("RoundRobinIndex");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("Frequency", frequency);
        compound.setInteger("TeleportMode", mode.ordinal());
        compound.setInteger("RoundRobinIndex", roundRobinIndex);
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
