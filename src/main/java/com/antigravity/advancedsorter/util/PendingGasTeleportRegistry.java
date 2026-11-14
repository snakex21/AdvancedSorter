package com.antigravity.advancedsorter.util;

import mekanism.api.gas.GasStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

import java.util.*;

/**
 * Registry for gases waiting to be delivered to unloaded chunks.
 * When a teleport gas pipe tries to send to an unloaded chunk, gases are queued here.
 * When the target chunk loads, the pipe retrieves its pending gases.
 */
public class PendingGasTeleportRegistry extends WorldSavedData {
    private static final String DATA_NAME = "AdvancedSorter_PendingGasTeleports";

    // Key: "dimension:x:y:z" -> List of pending gases
    private final Map<String, List<GasStack>> pendingGases = new HashMap<>();

    public PendingGasTeleportRegistry() {
        super(DATA_NAME);
    }

    public PendingGasTeleportRegistry(String name) {
        super(name);
    }

    public static PendingGasTeleportRegistry get(World world) {
        MapStorage storage = world.getMapStorage();
        if (storage == null) {
            return new PendingGasTeleportRegistry();
        }

        PendingGasTeleportRegistry instance = (PendingGasTeleportRegistry) storage.getOrLoadData(
                PendingGasTeleportRegistry.class, DATA_NAME);

        if (instance == null) {
            instance = new PendingGasTeleportRegistry();
            storage.setData(DATA_NAME, instance);
        }

        return instance;
    }

    private static String makeKey(int dimension, BlockPos pos) {
        return dimension + ":" + pos.getX() + ":" + pos.getY() + ":" + pos.getZ();
    }

    /**
     * Queue a gas for delivery to a position in an unloaded chunk.
     */
    public void queueGas(int dimension, BlockPos pos, GasStack gas) {
        if (gas == null || gas.amount <= 0) return;

        String key = makeKey(dimension, pos);
        List<GasStack> gases = pendingGases.computeIfAbsent(key, k -> new ArrayList<>());

        // Try to merge with existing gas of the same type
        boolean merged = false;
        for (GasStack existing : gases) {
            if (existing.getGas() == gas.getGas()) {
                existing.amount += gas.amount;
                merged = true;
                break;
            }
        }

        if (!merged) {
            gases.add(gas.copy());
        }

        markDirty();
    }

    /**
     * Check if there are pending gases for a position.
     */
    public boolean hasPendingGases(int dimension, BlockPos pos) {
        String key = makeKey(dimension, pos);
        List<GasStack> gases = pendingGases.get(key);
        return gases != null && !gases.isEmpty();
    }

    /**
     * Retrieve and remove all pending gases for a position.
     * Called when the chunk loads and the pipe becomes active.
     */
    public List<GasStack> retrievePendingGases(int dimension, BlockPos pos) {
        String key = makeKey(dimension, pos);
        List<GasStack> gases = pendingGases.remove(key);
        if (gases != null && !gases.isEmpty()) {
            markDirty();
            return gases;
        }
        return Collections.emptyList();
    }

    /**
     * Get total amount of pending gas (for debugging/GUI)
     */
    public int getPendingAmount(int dimension, BlockPos pos) {
        String key = makeKey(dimension, pos);
        List<GasStack> gases = pendingGases.get(key);
        if (gases == null) return 0;

        int total = 0;
        for (GasStack g : gases) {
            total += g.amount;
        }
        return total;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        pendingGases.clear();

        NBTTagList entries = nbt.getTagList("PendingGases", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < entries.tagCount(); i++) {
            NBTTagCompound entry = entries.getCompoundTagAt(i);
            String key = entry.getString("Key");

            List<GasStack> gases = new ArrayList<>();
            NBTTagList gasList = entry.getTagList("Gases", Constants.NBT.TAG_COMPOUND);
            for (int j = 0; j < gasList.tagCount(); j++) {
                GasStack gas = GasStack.readFromNBT(gasList.getCompoundTagAt(j));
                if (gas != null && gas.amount > 0) {
                    gases.add(gas);
                }
            }

            if (!gases.isEmpty()) {
                pendingGases.put(key, gases);
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList entries = new NBTTagList();

        for (Map.Entry<String, List<GasStack>> entry : pendingGases.entrySet()) {
            if (entry.getValue().isEmpty()) continue;

            NBTTagCompound entryNbt = new NBTTagCompound();
            entryNbt.setString("Key", entry.getKey());

            NBTTagList gasList = new NBTTagList();
            for (GasStack gas : entry.getValue()) {
                gasList.appendTag(gas.write(new NBTTagCompound()));
            }
            entryNbt.setTag("Gases", gasList);

            entries.appendTag(entryNbt);
        }

        nbt.setTag("PendingGases", entries);
        return nbt;
    }
}
