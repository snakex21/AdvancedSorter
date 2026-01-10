package com.antigravity.advancedsorter.network;

import com.antigravity.advancedsorter.client.gui.GuiInventoryIndex;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

public class PacketInventoryIndexSync implements IMessage {

    // Simple Data Structure for Sync
    public static class IndexEntry {
        public ItemStack stack;
        public int count;
        public List<BlockPos> locations;

        public IndexEntry(ItemStack stack, int count, List<BlockPos> locations) {
            this.stack = stack;
            this.count = count;
            this.locations = locations;
        }
    }

    public List<IndexEntry> entries;
    public BlockPos indexerPos; // Position of the indexer sending this data

    public PacketInventoryIndexSync() {
        this.entries = new ArrayList<>();
        this.indexerPos = BlockPos.ORIGIN;
    }

    public PacketInventoryIndexSync(BlockPos indexerPos, List<IndexEntry> entries) {
        this.indexerPos = indexerPos;
        this.entries = entries;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        // Read indexer position first
        this.indexerPos = BlockPos.fromLong(buf.readLong());

        int count = buf.readInt();
        entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ItemStack stack = ByteBufUtils.readItemStack(buf);
            int total = buf.readInt();
            int locCount = buf.readInt();
            List<BlockPos> locs = new ArrayList<>(locCount);
            for (int j = 0; j < locCount; j++) {
                locs.add(BlockPos.fromLong(buf.readLong()));
            }
            entries.add(new IndexEntry(stack, total, locs));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // Write indexer position first
        buf.writeLong(indexerPos.toLong());

        buf.writeInt(entries.size());
        for (IndexEntry entry : entries) {
            ByteBufUtils.writeItemStack(buf, entry.stack);
            buf.writeInt(entry.count);
            buf.writeInt(entry.locations.size());
            for (BlockPos pos : entry.locations) {
                buf.writeLong(pos.toLong());
            }
        }
    }

    public static class Handler implements IMessageHandler<PacketInventoryIndexSync, IMessage> {
        @Override
        public IMessage onMessage(PacketInventoryIndexSync message, MessageContext ctx) {
            // Client side handle
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (Minecraft.getMinecraft().currentScreen instanceof GuiInventoryIndex) {
                    GuiInventoryIndex gui = (GuiInventoryIndex) Minecraft.getMinecraft().currentScreen;
                    // Only update if this packet is for the indexer we're viewing
                    if (gui.isForIndexer(message.indexerPos)) {
                        gui.updateIndex(message.entries);
                    }
                }
            });
            return null;
        }
    }
}
