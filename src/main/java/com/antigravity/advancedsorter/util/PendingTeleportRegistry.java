package com.antigravity.advancedsorter.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

import java.util.*;

/**
 * Registry for items waiting to be delivered to unloaded chunks.
 * When a teleport pipe tries to send to an unloaded chunk, items are queued here.
 * When the target chunk loads, the pipe retrieves its pending items.
 */
public class PendingTeleportRegistry extends WorldSavedData {
    private static final String DATA_NAME = "AdvancedSorter_PendingTeleports";

    // Key: "dimension:x:y:z" -> List of pending items
    private final Map<String, List<ItemStack>> pendingItems = new HashMap<>();

    public PendingTeleportRegistry() {
        super(DATA_NAME);
    }

    public PendingTeleportRegistry(String name) {
        super(name);
    }

    public static PendingTeleportRegistry get(World world) {
        MapStorage storage = world.getMapStorage();
        if (storage == null) {
            return new PendingTeleportRegistry();
        }

        PendingTeleportRegistry instance = (PendingTeleportRegistry) storage.getOrLoadData(
                PendingTeleportRegistry.class, DATA_NAME);

        if (instance == null) {
            instance = new PendingTeleportRegistry();
            storage.setData(DATA_NAME, instance);
        }

        return instance;
    }

    private static String makeKey(int dimension, BlockPos pos) {
        return dimension + ":" + pos.getX() + ":" + pos.getY() + ":" + pos.getZ();
    }

    /**
     * Queue an item for delivery to a position in an unloaded chunk.
     */
    public void queueItem(int dimension, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) return;

        String key = makeKey(dimension, pos);
        pendingItems.computeIfAbsent(key, k -> new ArrayList<>()).add(stack.copy());
        markDirty();
    }

    /**
     * Check if there are pending items for a position.
     */
    public boolean hasPendingItems(int dimension, BlockPos pos) {
        String key = makeKey(dimension, pos);
        List<ItemStack> items = pendingItems.get(key);
        return items != null && !items.isEmpty();
    }

    /**
     * Retrieve and remove all pending items for a position.
     * Called when the chunk loads and the pipe becomes active.
     */
    public List<ItemStack> retrievePendingItems(int dimension, BlockPos pos) {
        String key = makeKey(dimension, pos);
        List<ItemStack> items = pendingItems.remove(key);
        if (items != null && !items.isEmpty()) {
            markDirty();
            return items;
        }
        return Collections.emptyList();
    }

    /**
     * Get count of pending items (for debugging/GUI)
     */
    public int getPendingCount(int dimension, BlockPos pos) {
        String key = makeKey(dimension, pos);
        List<ItemStack> items = pendingItems.get(key);
        return items != null ? items.size() : 0;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        pendingItems.clear();

        NBTTagList entries = nbt.getTagList("PendingItems", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < entries.tagCount(); i++) {
            NBTTagCompound entry = entries.getCompoundTagAt(i);
            String key = entry.getString("Key");

            List<ItemStack> items = new ArrayList<>();
            NBTTagList itemList = entry.getTagList("Items", Constants.NBT.TAG_COMPOUND);
            for (int j = 0; j < itemList.tagCount(); j++) {
                ItemStack stack = new ItemStack(itemList.getCompoundTagAt(j));
                if (!stack.isEmpty()) {
                    items.add(stack);
                }
            }

            if (!items.isEmpty()) {
                pendingItems.put(key, items);
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList entries = new NBTTagList();

        for (Map.Entry<String, List<ItemStack>> entry : pendingItems.entrySet()) {
            if (entry.getValue().isEmpty()) continue;

            NBTTagCompound entryNbt = new NBTTagCompound();
            entryNbt.setString("Key", entry.getKey());

            NBTTagList itemList = new NBTTagList();
            for (ItemStack stack : entry.getValue()) {
                itemList.appendTag(stack.writeToNBT(new NBTTagCompound()));
            }
            entryNbt.setTag("Items", itemList);

            entries.appendTag(entryNbt);
        }

        nbt.setTag("PendingItems", entries);
        return nbt;
    }
}
