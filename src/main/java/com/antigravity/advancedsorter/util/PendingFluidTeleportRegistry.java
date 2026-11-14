package com.antigravity.advancedsorter.util;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;

import java.util.*;

/**
 * Registry for fluids waiting to be delivered to unloaded chunks.
 * When a teleport fluid pipe tries to send to an unloaded chunk, fluids are queued here.
 * When the target chunk loads, the pipe retrieves its pending fluids.
 */
public class PendingFluidTeleportRegistry extends WorldSavedData {
    private static final String DATA_NAME = "AdvancedSorter_PendingFluidTeleports";

    // Key: "dimension:x:y:z" -> List of pending fluids
    private final Map<String, List<FluidStack>> pendingFluids = new HashMap<>();

    public PendingFluidTeleportRegistry() {
        super(DATA_NAME);
    }

    public PendingFluidTeleportRegistry(String name) {
        super(name);
    }

    public static PendingFluidTeleportRegistry get(World world) {
        MapStorage storage = world.getMapStorage();
        if (storage == null) {
            return new PendingFluidTeleportRegistry();
        }

        PendingFluidTeleportRegistry instance = (PendingFluidTeleportRegistry) storage.getOrLoadData(
                PendingFluidTeleportRegistry.class, DATA_NAME);

        if (instance == null) {
            instance = new PendingFluidTeleportRegistry();
            storage.setData(DATA_NAME, instance);
        }

        return instance;
    }

    private static String makeKey(int dimension, BlockPos pos) {
        return dimension + ":" + pos.getX() + ":" + pos.getY() + ":" + pos.getZ();
    }

    /**
     * Queue a fluid for delivery to a position in an unloaded chunk.
     */
    public void queueFluid(int dimension, BlockPos pos, FluidStack fluid) {
        if (fluid == null || fluid.amount <= 0) return;

        String key = makeKey(dimension, pos);
        List<FluidStack> fluids = pendingFluids.computeIfAbsent(key, k -> new ArrayList<>());

        // Try to merge with existing fluid of the same type
        boolean merged = false;
        for (FluidStack existing : fluids) {
            if (existing.isFluidEqual(fluid)) {
                existing.amount += fluid.amount;
                merged = true;
                break;
            }
        }

        if (!merged) {
            fluids.add(fluid.copy());
        }

        markDirty();
    }

    /**
     * Check if there are pending fluids for a position.
     */
    public boolean hasPendingFluids(int dimension, BlockPos pos) {
        String key = makeKey(dimension, pos);
        List<FluidStack> fluids = pendingFluids.get(key);
        return fluids != null && !fluids.isEmpty();
    }

    /**
     * Retrieve and remove all pending fluids for a position.
     * Called when the chunk loads and the pipe becomes active.
     */
    public List<FluidStack> retrievePendingFluids(int dimension, BlockPos pos) {
        String key = makeKey(dimension, pos);
        List<FluidStack> fluids = pendingFluids.remove(key);
        if (fluids != null && !fluids.isEmpty()) {
            markDirty();
            return fluids;
        }
        return Collections.emptyList();
    }

    /**
     * Get total amount of pending fluid (for debugging/GUI)
     */
    public int getPendingAmount(int dimension, BlockPos pos) {
        String key = makeKey(dimension, pos);
        List<FluidStack> fluids = pendingFluids.get(key);
        if (fluids == null) return 0;

        int total = 0;
        for (FluidStack f : fluids) {
            total += f.amount;
        }
        return total;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        pendingFluids.clear();

        NBTTagList entries = nbt.getTagList("PendingFluids", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < entries.tagCount(); i++) {
            NBTTagCompound entry = entries.getCompoundTagAt(i);
            String key = entry.getString("Key");

            List<FluidStack> fluids = new ArrayList<>();
            NBTTagList fluidList = entry.getTagList("Fluids", Constants.NBT.TAG_COMPOUND);
            for (int j = 0; j < fluidList.tagCount(); j++) {
                FluidStack fluid = FluidStack.loadFluidStackFromNBT(fluidList.getCompoundTagAt(j));
                if (fluid != null && fluid.amount > 0) {
                    fluids.add(fluid);
                }
            }

            if (!fluids.isEmpty()) {
                pendingFluids.put(key, fluids);
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList entries = new NBTTagList();

        for (Map.Entry<String, List<FluidStack>> entry : pendingFluids.entrySet()) {
            if (entry.getValue().isEmpty()) continue;

            NBTTagCompound entryNbt = new NBTTagCompound();
            entryNbt.setString("Key", entry.getKey());

            NBTTagList fluidList = new NBTTagList();
            for (FluidStack fluid : entry.getValue()) {
                fluidList.appendTag(fluid.writeToNBT(new NBTTagCompound()));
            }
            entryNbt.setTag("Fluids", fluidList);

            entries.appendTag(entryNbt);
        }

        nbt.setTag("PendingFluids", entries);
        return nbt;
    }
}
