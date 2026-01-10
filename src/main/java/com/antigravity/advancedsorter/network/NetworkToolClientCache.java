package com.antigravity.advancedsorter.network;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side cache for Network Tool data synced from server
 */
public class NetworkToolClientCache {

    public static List<BlockPos> markedChests = new ArrayList<>();
    public static BlockPos selectedIndexer = null;

    public static void clear() {
        markedChests.clear();
        selectedIndexer = null;
    }
}
