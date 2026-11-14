package com.antigravity.advancedsorter.pipes.fluid.extraction;

import com.antigravity.advancedsorter.pipes.fluid.FluidPipeTier;
import com.antigravity.advancedsorter.pipes.fluid.TileFluidPipe;
import com.antigravity.advancedsorter.util.PumpRegistry;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.EnumMap;
import java.util.Map;

/**
 * Extraction fluid pipe that actively pulls fluids from adjacent tanks.
 * Controlled remotely by pump controller blocks using frequency system,
 * or can be set to manual mode for always-on extraction.
 */
public class TileExtractionFluidPipe extends TileFluidPipe {

    private int frequency = 0;
    private int lastRegisteredFrequency = -1;
    private boolean manualMode = false;

    // OPTYMALIZACJA: Cache dla IFluidHandler sąsiadów (pomijając inne rury)
    private final Map<EnumFacing, IFluidHandler> extractionHandlerCache = new EnumMap<>(EnumFacing.class);
    // OPTYMALIZACJA: Lista do iteracji (unikamy alokacji)
    private final java.util.List<EnumFacing> cachedExtractionFaces = new java.util.ArrayList<>();
    private boolean extractionCacheDirty = true;

    // OPTYMALIZACJA: Round-Robin
    private int roundRobinIndex = 0;

    // OPTYMALIZACJA: Sleep timer
    private int sleepTimer = 0;

    public TileExtractionFluidPipe() {
        super(FluidPipeTier.IRON);
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) {
            return;
        }

        if (sleepTimer > 0) {
            sleepTimer--;
        }

        // Register/update in pump registry
        if (lastRegisteredFrequency != frequency) {
            PumpRegistry registry = PumpRegistry.get(world);
            if (lastRegisteredFrequency >= 0) {
                registry.unregisterExtractionPipe(lastRegisteredFrequency, pos);
            }
            registry.registerExtractionPipe(frequency, pos);
            lastRegisteredFrequency = frequency;
        }

        // Check if pumping is enabled: manual mode OR pump controller enabled
        boolean shouldExtract = manualMode;
        if (!shouldExtract) {
            PumpRegistry registry = PumpRegistry.get(world);
            shouldExtract = registry.isPumpingEnabled(frequency);
        }

        if (shouldExtract && sleepTimer <= 0) {
            // Extract from adjacent tanks
            if (!extractFromNeighbors()) {
                sleepTimer = 20; // Sleep if nothing extracted
            }
        }

        // Normal pipe behavior (distribution)
        super.update();
    }

    private boolean extractFromNeighbors() {
        if (tank.getFluidAmount() >= tank.getCapacity()) {
            return false; // Tank is full
        }

        // OPTYMALIZACJA: Aktualizuj cache tylko gdy potrzeba
        if (extractionCacheDirty) {
            updateExtractionCache();
        }

        if (cachedExtractionFaces.isEmpty()) return false;

        int extractRate = tier.getTransferRate();
        boolean extractedAny = false;

        // Round-Robin iteration
        int size = cachedExtractionFaces.size();
        int startIdx = roundRobinIndex % size;

        for (int i = 0; i < size; i++) {
            int idx = (startIdx + i) % size;
            EnumFacing face = cachedExtractionFaces.get(idx);
            IFluidHandler handler = extractionHandlerCache.get(face);

            if (handler != null) {
                // Try to extract
                FluidStack drained = handler.drain(extractRate, false);
                if (drained != null && drained.amount > 0) {
                    int filled = tank.fill(drained, true);
                    if (filled > 0) {
                        handler.drain(filled, true);
                        markDirty();
                        extractedAny = true;

                        // Move RR index to next for fair extraction
                        roundRobinIndex = (idx + 1) % size;
                        break; // One extraction per tick
                    }
                }
            }
        }

        return extractedAny;
    }

    private void updateExtractionCache() {
        extractionCacheDirty = false;
        extractionHandlerCache.clear();
        cachedExtractionFaces.clear();

        for (EnumFacing face : EnumFacing.VALUES) {
            TileEntity neighbor = world.getTileEntity(pos.offset(face));
            if (neighbor == null)
                continue;

            // Don't extract from other pipes
            if (neighbor instanceof TileFluidPipe)
                continue;

            IFluidHandler handler = neighbor.getCapability(
                    CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face.getOpposite());

            if (handler != null) {
                extractionHandlerCache.put(face, handler);
                cachedExtractionFaces.add(face);
            }
        }
    }

    @Override
    protected void updateConnections() {
        super.updateConnections();
        // Invalidate extraction cache when connections change
        extractionCacheDirty = true;
    }

    /**
     * Wywołaj gdy sąsiad się zmieni
     */
    public void onNeighborChanged() {
        extractionCacheDirty = true;
        markConnectionsDirty();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (world != null && !world.isRemote && lastRegisteredFrequency >= 0) {
            PumpRegistry.get(world).unregisterExtractionPipe(lastRegisteredFrequency, pos);
        }
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = Math.max(0, Math.min(99, frequency));
        markDirty();
        sendUpdate();
    }

    public boolean isManualMode() {
        return manualMode;
    }

    public void setManualMode(boolean manualMode) {
        this.manualMode = manualMode;
        markDirty();
        sendUpdate();
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        frequency = compound.getInteger("frequency");
        manualMode = compound.getBoolean("manualMode");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("frequency", frequency);
        compound.setBoolean("manualMode", manualMode);
        return compound;
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.getUpdateTag();
        tag.setInteger("frequency", frequency);
        tag.setBoolean("manualMode", manualMode);
        return tag;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        super.handleUpdateTag(tag);
        frequency = tag.getInteger("frequency");
        manualMode = tag.getBoolean("manualMode");
    }
}
