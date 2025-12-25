package com.antigravity.advancedsorter.util;

import com.antigravity.advancedsorter.AdvancedSorterMod;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.LoadingCallback;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles chunk loading for teleport pipes.
 * Keeps chunks with teleport pipes loaded so they can operate across
 * dimensions.
 */
public class ChunkLoadingHandler implements LoadingCallback {

    private static ChunkLoadingHandler INSTANCE;

    // Map of BlockPos -> Ticket for active chunk loaders
    private final Map<BlockPos, Ticket> activeTickets = new HashMap<>();

    public static ChunkLoadingHandler getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ChunkLoadingHandler();
        }
        return INSTANCE;
    }

    public static void register() {
        ForgeChunkManager.setForcedChunkLoadingCallback(AdvancedSorterMod.instance, getInstance());
    }

    @Override
    public void ticketsLoaded(List<Ticket> tickets, World world) {
        // Re-establish chunk loading from saved tickets
        for (Ticket ticket : tickets) {
            NBTTagCompound data = ticket.getModData();
            if (data.hasKey("PipeX")) {
                int x = data.getInteger("PipeX");
                int y = data.getInteger("PipeY");
                int z = data.getInteger("PipeZ");
                BlockPos pos = new BlockPos(x, y, z);

                // Check if the tile entity still exists
                if (world.isBlockLoaded(pos) || forceLoadAndCheck(ticket, pos, world)) {
                    activeTickets.put(pos, ticket);
                    ChunkPos chunkPos = new ChunkPos(pos);
                    ForgeChunkManager.forceChunk(ticket, chunkPos);
                    System.out.println("[ChunkLoader] Restored chunk loading for teleport pipe at " + pos);
                } else {
                    // Tile entity no longer exists, release ticket
                    ForgeChunkManager.releaseTicket(ticket);
                }
            }
        }
    }

    private boolean forceLoadAndCheck(Ticket ticket, BlockPos pos, World world) {
        ChunkPos chunkPos = new ChunkPos(pos);
        ForgeChunkManager.forceChunk(ticket, chunkPos);
        return world.getTileEntity(pos) != null;
    }

    /**
     * Request chunk loading for a teleport pipe at the given position.
     */
    public void requestChunkLoading(World world, BlockPos pos) {
        if (world.isRemote)
            return;

        // Already have a ticket for this position
        if (activeTickets.containsKey(pos)) {
            return;
        }

        Ticket ticket = ForgeChunkManager.requestTicket(
                AdvancedSorterMod.instance,
                world,
                Type.NORMAL);

        if (ticket == null) {
            System.err.println("[ChunkLoader] Could not get chunk loading ticket for " + pos);
            return;
        }

        // Store position in ticket data for persistence
        NBTTagCompound data = ticket.getModData();
        data.setInteger("PipeX", pos.getX());
        data.setInteger("PipeY", pos.getY());
        data.setInteger("PipeZ", pos.getZ());

        // Force load the chunk
        ChunkPos chunkPos = new ChunkPos(pos);
        ForgeChunkManager.forceChunk(ticket, chunkPos);

        activeTickets.put(pos, ticket);
        System.out.println("[ChunkLoader] Started chunk loading for teleport pipe at " + pos);
    }

    /**
     * Release chunk loading for a teleport pipe at the given position.
     */
    public void releaseChunkLoading(World world, BlockPos pos) {
        if (world.isRemote)
            return;

        Ticket ticket = activeTickets.remove(pos);
        if (ticket != null) {
            ForgeChunkManager.releaseTicket(ticket);
            System.out.println("[ChunkLoader] Released chunk loading for teleport pipe at " + pos);
        }
    }

    /**
     * Check if chunk loading is active for a position.
     */
    public boolean isChunkLoadingActive(BlockPos pos) {
        return activeTickets.containsKey(pos);
    }
}
