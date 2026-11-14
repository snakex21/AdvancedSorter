package com.antigravity.advancedsorter.pipes.fluid.teleport;

import com.antigravity.advancedsorter.pipes.fluid.FluidPipeTier;
import com.antigravity.advancedsorter.pipes.fluid.TileFluidPipe;
import com.antigravity.advancedsorter.network.PacketTeleportInfo;
import com.antigravity.advancedsorter.util.TeleportRegistry;
import com.antigravity.advancedsorter.util.PendingFluidTeleportRegistry;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.BlockPos;

/**
 * Teleport Fluid Pipe - transfers fluids across dimensions.
 */
public class TileTeleportFluidPipe extends TileFluidPipe {

    private int frequency = 0;
    private TeleportMode mode = TeleportMode.BOTH;
    private int roundRobinIndex = 0;

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

    public TileTeleportFluidPipe() {
        super(FluidPipeTier.DIAMOND);
    }

    // OPTYMALIZACJA: Sleep timer dla rur teleportacyjnych
    private int sleepTimer = 0;

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

        // OPTYMALIZACJA: Chunk loading tylko dla SEND mode
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

            // Odbierz oczekujące płyny jeśli teraz może odbierać
            if (mode.canReceive() && !oldMode.canReceive()) {
                retrievePendingFluids();
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
            // OPTYMALIZACJA: Chunk loading tylko dla rur WYSYŁAJĄCYCH
            if (mode.canSend()) {
                com.antigravity.advancedsorter.util.ChunkLoadingHandler.getInstance()
                        .requestChunkLoading(world, pos);
            }
            // Odbierz oczekujące płyny (wysłane gdy chunk był niezaładowany)
            if (mode.canReceive()) {
                retrievePendingFluids();
            }
        }
    }

    /**
     * Odbiera płyny które były wysłane gdy ten chunk był niezaładowany.
     */
    private void retrievePendingFluids() {
        if (world == null || world.isRemote) return;
        if (!mode.canReceive()) return;

        PendingFluidTeleportRegistry pending = PendingFluidTeleportRegistry.get(world);
        List<FluidStack> fluids = pending.retrievePendingFluids(
                world.provider.getDimension(), pos);

        for (FluidStack fluid : fluids) {
            if (fluid != null && fluid.amount > 0) {
                tank.fill(fluid, true);
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
            TeleportRegistry.get(world).removeFluidPipe(pos, world.provider.getDimension());
            // Zwolnij chunk loading jeśli rura wysyłała
            if (mode.canSend()) {
                com.antigravity.advancedsorter.util.ChunkLoadingHandler.getInstance()
                        .releaseChunkLoading(world, pos);
            }
        }
    }

    public void updateRegistry() {
        if (world != null && !world.isRemote) {
            TeleportRegistry.get(world).registerFluidPipe(frequency, world.provider.getDimension(), pos,
                    mode.canSend(), mode.canReceive());
        }
    }

    @Override
    public void update() {
        if (world == null || world.isRemote)
            return;

        if (sleepTimer > 0) {
            sleepTimer--;
            // Nadal wywołuj super.update() bo rura może odbierać płyn od sąsiadów
            // Ale jeśli jest pusta i śpi, super.update() (TileFluidPipe) też powinna być zoptymalizowana.
        }

        super.update();

        // Try to teleport fluid if we can send
        if (mode.canSend() && tank.getFluidAmount() > 0 && sleepTimer <= 0) {
            if (!tryTeleportFluid()) {
                sleepTimer = 20; // Śpij jeśli brak odbiorników
            }
        }
    }

    /**
     * @return true if fluid was sent or queued
     */
    private boolean tryTeleportFluid() {
        TeleportRegistry registry = TeleportRegistry.get(world);
        List<TeleportRegistry.TeleportLocation> receivers = registry.getFluidReceivers(frequency);

        if (receivers.isEmpty()) {
            return false;
        }

        int amountToSend = Math.min(tank.getFluidAmount(), tier.getTransferRate());

        // Ensure index is within bounds
        if (roundRobinIndex >= receivers.size()) {
            roundRobinIndex = 0;
        }

        int attempts = 0;
        int size = receivers.size();

        // Try to find a receiver using Round-Robin approach
        while (attempts < size) {
            int currentIndex = (roundRobinIndex + attempts) % size;
            TeleportRegistry.TeleportLocation loc = receivers.get(currentIndex);
            attempts++;

            if (loc.dimension == world.provider.getDimension() && loc.pos.equals(pos)) {
                continue; // Skip self
            }

            // Pobierz świat docelowy
            WorldServer targetWorld = DimensionManager.getWorld(loc.dimension);

            // OPTYMALIZACJA: Jeśli wymiar nie jest załadowany - kolejkuj płyn
            if (targetWorld == null) {
                FluidStack toQueue = tank.drain(amountToSend, true);
                if (toQueue != null && toQueue.amount > 0) {
                    PendingFluidTeleportRegistry.get(world).queueFluid(loc.dimension, loc.pos, toQueue);
                    roundRobinIndex = (currentIndex + 1) % size;
                    markDirty();
                    requestClientSync();
                    return true; // Płyn "wysłany" (do kolejki)
                }
                continue;
            }

            // OPTYMALIZACJA: Jeśli chunk nie jest załadowany - kolejkuj płyn
            if (!targetWorld.isBlockLoaded(loc.pos)) {
                FluidStack toQueue = tank.drain(amountToSend, true);
                if (toQueue != null && toQueue.amount > 0) {
                    PendingFluidTeleportRegistry.get(world).queueFluid(loc.dimension, loc.pos, toQueue);
                    roundRobinIndex = (currentIndex + 1) % size;
                    markDirty();
                    requestClientSync();
                    return true; // Płyn "wysłany" (do kolejki)
                }
                continue;
            }

            // Chunk jest załadowany - teleportuj bezpośrednio
            TileEntity te = targetWorld.getTileEntity(loc.pos);
            if (te instanceof TileTeleportFluidPipe) {
                TileTeleportFluidPipe targetPipe = (TileTeleportFluidPipe) te;

                if (targetPipe.getFrequency() != frequency) {
                    registry.removeFluidPipe(loc.pos, loc.dimension);
                    continue;
                }
                if (!targetPipe.getMode().canReceive()) {
                    continue;
                }

                // Transfer fluid
                FluidStack toTransfer = tank.drain(amountToSend, false);
                if (toTransfer != null && toTransfer.amount > 0) {
                    int filled = targetPipe.tank.fill(toTransfer, true);
                    if (filled > 0) {
                        tank.drain(filled, true);
                        targetPipe.markDirty();
                        targetPipe.requestClientSync();
                        markDirty();
                        this.requestClientSync();

                        roundRobinIndex = (currentIndex + 1) % size;
                        return true; // Only transfer to one receiver per tick
                    }
                }
            } else {
                registry.removeFluidPipe(loc.pos, loc.dimension);
            }
        }
        return false;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        frequency = compound.getInteger("Frequency");
        mode = TeleportMode.values()[compound.getInteger("Mode") % TeleportMode.values().length];
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("Frequency", frequency);
        compound.setInteger("Mode", mode.ordinal());
        return compound;
    }

    public void syncConnectionInfo() {
        if (world != null && !world.isRemote) {
            TeleportRegistry registry = TeleportRegistry.get(world);
            List<TeleportRegistry.TeleportLocation> receivers = registry.getFluidReceivers(frequency);
            List<TeleportRegistry.TeleportLocation> senders = registry.getFluidSenders(frequency);

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
                        com.antigravity.advancedsorter.AdvancedSorterMod.network.sendTo(packet,
                                (EntityPlayerMP) player);
                    }
                }
            }
        }
    }
}
