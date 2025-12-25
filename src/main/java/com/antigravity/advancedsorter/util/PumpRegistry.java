package com.antigravity.advancedsorter.util;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for managing extraction pipes and their pump status by frequency.
 * Saved per-dimension in world data.
 */
public class PumpRegistry extends WorldSavedData {

    private static final String DATA_NAME = "advancedsorter_pump_registry";

    // Maps frequency -> list of extraction pipe positions
    private final Map<Integer, List<BlockPos>> extractionPipes = new HashMap<>();
    // Maps frequency -> pumping enabled
    private final Map<Integer, Boolean> pumpingEnabled = new HashMap<>();

    public PumpRegistry() {
        super(DATA_NAME);
    }

    public PumpRegistry(String name) {
        super(name);
    }

    public static PumpRegistry get(World world) {
        MapStorage storage = world.getMapStorage();
        PumpRegistry instance = (PumpRegistry) storage.getOrLoadData(PumpRegistry.class, DATA_NAME);
        if (instance == null) {
            instance = new PumpRegistry();
            storage.setData(DATA_NAME, instance);
        }
        return instance;
    }

    /**
     * Register an extraction pipe at the given position with the given frequency.
     */
    public void registerExtractionPipe(int frequency, BlockPos pos) {
        List<BlockPos> pipes = extractionPipes.computeIfAbsent(frequency, k -> new ArrayList<>());
        if (!pipes.contains(pos)) {
            pipes.add(pos);
            markDirty();
        }
    }

    /**
     * Unregister an extraction pipe.
     */
    public void unregisterExtractionPipe(int frequency, BlockPos pos) {
        List<BlockPos> pipes = extractionPipes.get(frequency);
        if (pipes != null) {
            pipes.remove(pos);
            if (pipes.isEmpty()) {
                extractionPipes.remove(frequency);
            }
            markDirty();
        }
    }

    /**
     * Update the frequency of an extraction pipe (re-register with new frequency).
     */
    public void updateFrequency(int oldFreq, int newFreq, BlockPos pos) {
        unregisterExtractionPipe(oldFreq, pos);
        registerExtractionPipe(newFreq, pos);
    }

    /**
     * Check if pumping is enabled for a given frequency.
     */
    public boolean isPumpingEnabled(int frequency) {
        return pumpingEnabled.getOrDefault(frequency, false);
    }

    /**
     * Set pumping state for a frequency.
     */
    public void setPumpingEnabled(int frequency, boolean enabled) {
        pumpingEnabled.put(frequency, enabled);
        markDirty();
    }

    /**
     * Get count of extraction pipes with given frequency.
     */
    public int getPipeCount(int frequency) {
        List<BlockPos> pipes = extractionPipes.get(frequency);
        return pipes != null ? pipes.size() : 0;
    }

    /**
     * Get all extraction pipe positions for a frequency.
     */
    public List<BlockPos> getExtractionPipes(int frequency) {
        return extractionPipes.getOrDefault(frequency, new ArrayList<>());
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        extractionPipes.clear();
        pumpingEnabled.clear();

        // Read extraction pipes
        NBTTagList pipesList = nbt.getTagList("extractionPipes", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < pipesList.tagCount(); i++) {
            NBTTagCompound entry = pipesList.getCompoundTagAt(i);
            int freq = entry.getInteger("freq");
            NBTTagList posList = entry.getTagList("positions", Constants.NBT.TAG_COMPOUND);
            List<BlockPos> positions = new ArrayList<>();
            for (int j = 0; j < posList.tagCount(); j++) {
                NBTTagCompound posTag = posList.getCompoundTagAt(j);
                positions.add(new BlockPos(
                        posTag.getInteger("x"),
                        posTag.getInteger("y"),
                        posTag.getInteger("z")));
            }
            extractionPipes.put(freq, positions);
        }

        // Read pumping states
        NBTTagList pumpingList = nbt.getTagList("pumpingStates", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < pumpingList.tagCount(); i++) {
            NBTTagCompound entry = pumpingList.getCompoundTagAt(i);
            pumpingEnabled.put(entry.getInteger("freq"), entry.getBoolean("enabled"));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        // Write extraction pipes
        NBTTagList pipesList = new NBTTagList();
        for (Map.Entry<Integer, List<BlockPos>> entry : extractionPipes.entrySet()) {
            NBTTagCompound freqEntry = new NBTTagCompound();
            freqEntry.setInteger("freq", entry.getKey());
            NBTTagList posList = new NBTTagList();
            for (BlockPos pos : entry.getValue()) {
                NBTTagCompound posTag = new NBTTagCompound();
                posTag.setInteger("x", pos.getX());
                posTag.setInteger("y", pos.getY());
                posTag.setInteger("z", pos.getZ());
                posList.appendTag(posTag);
            }
            freqEntry.setTag("positions", posList);
            pipesList.appendTag(freqEntry);
        }
        nbt.setTag("extractionPipes", pipesList);

        // Write pumping states
        NBTTagList pumpingList = new NBTTagList();
        for (Map.Entry<Integer, Boolean> entry : pumpingEnabled.entrySet()) {
            NBTTagCompound stateEntry = new NBTTagCompound();
            stateEntry.setInteger("freq", entry.getKey());
            stateEntry.setBoolean("enabled", entry.getValue());
            pumpingList.appendTag(stateEntry);
        }
        nbt.setTag("pumpingStates", pumpingList);

        return nbt;
    }
}
