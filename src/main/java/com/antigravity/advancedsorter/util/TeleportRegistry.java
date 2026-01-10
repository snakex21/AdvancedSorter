package com.antigravity.advancedsorter.util;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

import java.util.*;

/**
 * Global registry for teleportation pipes.
 * Tracks all teleport pipes and their frequencies across dimensions.
 */
public class TeleportRegistry extends WorldSavedData {
    private static final String DATA_NAME = "AdvancedSorter_TeleportRegistry";

    // Frequency -> List of pipe locations
    private final Map<Integer, List<TeleportLocation>> pipesByFrequency = new HashMap<>();
    private final Map<Integer, List<TeleportLocation>> fluidPipesByFrequency = new HashMap<>();
    private final Map<Integer, List<TeleportLocation>> gasPipesByFrequency = new HashMap<>();

    public TeleportRegistry() {
        super(DATA_NAME);
    }

    public TeleportRegistry(String name) {
        super(name);
    }

    public static TeleportRegistry get(World world) {
        World overworld = world.getMinecraftServer().getWorld(0);
        MapStorage storage = overworld.getMapStorage();
        TeleportRegistry instance = (TeleportRegistry) storage.getOrLoadData(TeleportRegistry.class, DATA_NAME);

        if (instance == null) {
            instance = new TeleportRegistry();
            storage.setData(DATA_NAME, instance);
        }
        return instance;
    }

    // ========== Item Pipe Methods ==========

    public void registerPipe(int frequency, int dimension, BlockPos pos, boolean canSend, boolean canReceive) {
        removePipe(pos, dimension); // Remove old entry if exists

        pipesByFrequency.computeIfAbsent(frequency, k -> new ArrayList<>())
                .add(new TeleportLocation(dimension, pos, canSend, canReceive));
        markDirty();
    }

    public void removePipe(BlockPos pos, int dimension) {
        for (List<TeleportLocation> locations : pipesByFrequency.values()) {
            locations.removeIf(loc -> loc.pos.equals(pos) && loc.dimension == dimension);
        }
        markDirty();
    }

    public List<TeleportLocation> getReceivers(int frequency) {
        List<TeleportLocation> all = pipesByFrequency.get(frequency);
        if (all == null)
            return Collections.emptyList();

        List<TeleportLocation> receivers = new ArrayList<>();
        for (TeleportLocation loc : all) {
            if (loc.canReceive) {
                receivers.add(loc);
            }
        }
        return receivers;
    }

    public List<TeleportLocation> getSenders(int frequency) {
        List<TeleportLocation> all = pipesByFrequency.get(frequency);
        if (all == null)
            return Collections.emptyList();

        List<TeleportLocation> senders = new ArrayList<>();
        for (TeleportLocation loc : all) {
            if (loc.canSend) {
                senders.add(loc);
            }
        }
        return senders;
    }

    // ========== Fluid Pipe Methods ==========

    public void registerFluidPipe(int frequency, int dimension, BlockPos pos, boolean canSend, boolean canReceive) {
        removeFluidPipe(pos, dimension);

        fluidPipesByFrequency.computeIfAbsent(frequency, k -> new ArrayList<>())
                .add(new TeleportLocation(dimension, pos, canSend, canReceive));
        markDirty();
    }

    public void removeFluidPipe(BlockPos pos, int dimension) {
        for (List<TeleportLocation> locations : fluidPipesByFrequency.values()) {
            locations.removeIf(loc -> loc.pos.equals(pos) && loc.dimension == dimension);
        }
        markDirty();
    }

    public List<TeleportLocation> getFluidReceivers(int frequency) {
        List<TeleportLocation> all = fluidPipesByFrequency.get(frequency);
        if (all == null)
            return Collections.emptyList();

        List<TeleportLocation> receivers = new ArrayList<>();
        for (TeleportLocation loc : all) {
            if (loc.canReceive) {
                receivers.add(loc);
            }
        }
        return receivers;
    }

    public List<TeleportLocation> getFluidSenders(int frequency) {
        List<TeleportLocation> all = fluidPipesByFrequency.get(frequency);
        if (all == null)
            return Collections.emptyList();

        List<TeleportLocation> senders = new ArrayList<>();
        for (TeleportLocation loc : all) {
            if (loc.canSend) {
                senders.add(loc);
            }
        }
        return senders;
    }

    public Set<Integer> getFluidFrequencies() {
        return fluidPipesByFrequency.keySet();
    }

    public List<TeleportLocation> getFluidPipesByFrequency(int frequency) {
        return fluidPipesByFrequency.getOrDefault(frequency, Collections.emptyList());
    }

    // ========== Gas Pipe Methods ==========

    public void registerGasPipe(int frequency, int dimension, BlockPos pos, boolean canSend, boolean canReceive) {
        removeGasPipe(pos, dimension);

        gasPipesByFrequency.computeIfAbsent(frequency, k -> new ArrayList<>())
                .add(new TeleportLocation(dimension, pos, canSend, canReceive));
        markDirty();
    }

    public void removeGasPipe(BlockPos pos, int dimension) {
        for (List<TeleportLocation> locations : gasPipesByFrequency.values()) {
            locations.removeIf(loc -> loc.pos.equals(pos) && loc.dimension == dimension);
        }
        markDirty();
    }

    public List<TeleportLocation> getGasReceivers(int frequency) {
        List<TeleportLocation> all = gasPipesByFrequency.get(frequency);
        if (all == null)
            return Collections.emptyList();

        List<TeleportLocation> receivers = new ArrayList<>();
        for (TeleportLocation loc : all) {
            if (loc.canReceive) {
                receivers.add(loc);
            }
        }
        return receivers;
    }

    public List<TeleportLocation> getGasSenders(int frequency) {
        List<TeleportLocation> all = gasPipesByFrequency.get(frequency);
        if (all == null)
            return Collections.emptyList();

        List<TeleportLocation> senders = new ArrayList<>();
        for (TeleportLocation loc : all) {
            if (loc.canSend) {
                senders.add(loc);
            }
        }
        return senders;
    }

    public Set<Integer> getGasFrequencies() {
        return gasPipesByFrequency.keySet();
    }

    public List<TeleportLocation> getGasPipesByFrequency(int frequency) {
        return gasPipesByFrequency.getOrDefault(frequency, Collections.emptyList());
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        pipesByFrequency.clear();
        NBTTagList list = nbt.getTagList("Frequencies", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound freqTag = list.getCompoundTagAt(i);
            int freq = freqTag.getInteger("Freq");
            NBTTagList locsList = freqTag.getTagList("Locs", Constants.NBT.TAG_COMPOUND);
            List<TeleportLocation> locs = new ArrayList<>();
            for (int j = 0; j < locsList.tagCount(); j++) {
                locs.add(TeleportLocation.fromNBT(locsList.getCompoundTagAt(j)));
            }
            pipesByFrequency.put(freq, locs);
        }

        fluidPipesByFrequency.clear();
        NBTTagList fluidList = nbt.getTagList("FluidFrequencies", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < fluidList.tagCount(); i++) {
            NBTTagCompound freqTag = fluidList.getCompoundTagAt(i);
            int freq = freqTag.getInteger("Freq");
            NBTTagList locsList = freqTag.getTagList("Locs", Constants.NBT.TAG_COMPOUND);
            List<TeleportLocation> locs = new ArrayList<>();
            for (int j = 0; j < locsList.tagCount(); j++) {
                locs.add(TeleportLocation.fromNBT(locsList.getCompoundTagAt(j)));
            }
            fluidPipesByFrequency.put(freq, locs);
        }

        gasPipesByFrequency.clear();
        NBTTagList gasList = nbt.getTagList("GasFrequencies", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < gasList.tagCount(); i++) {
            NBTTagCompound freqTag = gasList.getCompoundTagAt(i);
            int freq = freqTag.getInteger("Freq");
            NBTTagList locsList = freqTag.getTagList("Locs", Constants.NBT.TAG_COMPOUND);
            List<TeleportLocation> locs = new ArrayList<>();
            for (int j = 0; j < locsList.tagCount(); j++) {
                locs.add(TeleportLocation.fromNBT(locsList.getCompoundTagAt(j)));
            }
            gasPipesByFrequency.put(freq, locs);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<Integer, List<TeleportLocation>> entry : pipesByFrequency.entrySet()) {
            NBTTagCompound freqTag = new NBTTagCompound();
            freqTag.setInteger("Freq", entry.getKey());
            NBTTagList locsList = new NBTTagList();
            for (TeleportLocation loc : entry.getValue()) {
                locsList.appendTag(loc.toNBT());
            }
            freqTag.setTag("Locs", locsList);
            list.appendTag(freqTag);
        }
        nbt.setTag("Frequencies", list);

        NBTTagList fluidList = new NBTTagList();
        for (Map.Entry<Integer, List<TeleportLocation>> entry : fluidPipesByFrequency.entrySet()) {
            NBTTagCompound freqTag = new NBTTagCompound();
            freqTag.setInteger("Freq", entry.getKey());
            NBTTagList locsList = new NBTTagList();
            for (TeleportLocation loc : entry.getValue()) {
                locsList.appendTag(loc.toNBT());
            }
            freqTag.setTag("Locs", locsList);
            fluidList.appendTag(freqTag);
        }
        nbt.setTag("FluidFrequencies", fluidList);

        NBTTagList gasList = new NBTTagList();
        for (Map.Entry<Integer, List<TeleportLocation>> entry : gasPipesByFrequency.entrySet()) {
            NBTTagCompound freqTag = new NBTTagCompound();
            freqTag.setInteger("Freq", entry.getKey());
            NBTTagList locsList = new NBTTagList();
            for (TeleportLocation loc : entry.getValue()) {
                locsList.appendTag(loc.toNBT());
            }
            freqTag.setTag("Locs", locsList);
            gasList.appendTag(freqTag);
        }
        nbt.setTag("GasFrequencies", gasList);

        return nbt;
    }

    public static class TeleportLocation {
        public final int dimension;
        public final BlockPos pos;
        public final boolean canSend;
        public final boolean canReceive;

        public TeleportLocation(int dimension, BlockPos pos, boolean canSend, boolean canReceive) {
            this.dimension = dimension;
            this.pos = pos;
            this.canSend = canSend;
            this.canReceive = canReceive;
        }

        public NBTTagCompound toNBT() {
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setInteger("Dim", dimension);
            nbt.setLong("Pos", pos.toLong());
            nbt.setBoolean("Snd", canSend);
            nbt.setBoolean("Rec", canReceive);
            return nbt;
        }

        public static TeleportLocation fromNBT(NBTTagCompound nbt) {
            return new TeleportLocation(
                    nbt.getInteger("Dim"),
                    BlockPos.fromLong(nbt.getLong("Pos")),
                    nbt.getBoolean("Snd"),
                    nbt.getBoolean("Rec"));
        }
    }
}
