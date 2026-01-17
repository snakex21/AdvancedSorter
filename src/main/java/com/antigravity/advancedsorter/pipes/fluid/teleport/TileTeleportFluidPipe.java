package com.antigravity.advancedsorter.pipes.fluid.teleport;

import com.antigravity.advancedsorter.pipes.fluid.FluidPipeTier;
import com.antigravity.advancedsorter.pipes.fluid.TileFluidPipe;
import com.antigravity.advancedsorter.network.PacketTeleportInfo;
import com.antigravity.advancedsorter.util.TeleportRegistry;
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
        if (world != null && !world.isRemote) {
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
            TeleportRegistry.get(world).removeFluidPipe(pos, world.provider.getDimension());
            // Release chunk loading
            com.antigravity.advancedsorter.util.ChunkLoadingHandler.getInstance()
                    .releaseChunkLoading(world, pos);
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

        super.update();

        // Try to teleport fluid if we can send
        if (mode.canSend() && tank.getFluidAmount() > 0) {
            tryTeleportFluid();
        }
    }

    private void tryTeleportFluid() {
        TeleportRegistry registry = TeleportRegistry.get(world);
        List<TeleportRegistry.TeleportLocation> receivers = registry.getFluidReceivers(frequency);

        // If no receivers found in registry, try to find and register any pipes
        // that should be on this frequency but weren't registered yet (world load issue)
        if (receivers.isEmpty()) {
            // Cleanup and re-validate registry periodically
            registry.validateAndCleanup(world);
            return;
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
                if (te instanceof TileTeleportFluidPipe) {
                    TileTeleportFluidPipe targetPipe = (TileTeleportFluidPipe) te;

                    // Force target to register if not in registry or frequency mismatch
                    if (targetPipe.getFrequency() != frequency) {
                        // Registry has stale data - remove invalid entry
                        registry.removeFluidPipe(loc.pos, loc.dimension);
                        continue;
                    }
                    if (!targetPipe.getMode().canReceive()) {
                        continue;
                    }

                    // Ensure target is registered (handles world load race condition)
                    targetPipe.updateRegistry();

                    // Transfer fluid
                    FluidStack toTransfer = tank.drain(amountToSend, false);
                    if (toTransfer != null && toTransfer.amount > 0) {
                        int filled = targetPipe.tank.fill(toTransfer, true);
                        if (filled > 0) {
                            tank.drain(filled, true);
                            targetPipe.markDirty();
                            targetPipe.requestClientSync(); // Use rate-limited sync to prevent packet spam
                            markDirty();
                            this.requestClientSync(); // Sync local tank changes too

                            // Success! Update index to start from the next one next time
                            roundRobinIndex = (currentIndex + 1) % size;
                            return; // Only transfer to one receiver per tick
                        }
                    }
                } else {
                    // Tile entity is not a teleport pipe - remove from registry
                    registry.removeFluidPipe(loc.pos, loc.dimension);
                }
            }
        }
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
