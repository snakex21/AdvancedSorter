package com.antigravity.advancedsorter.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Syncs Network Tool data (marked chests and selected indexer) from server to client
 */
public class PacketNetworkToolSync implements IMessage {

    private List<BlockPos> markedChests;
    private BlockPos selectedIndexer;
    private boolean hasSelectedIndexer;

    public PacketNetworkToolSync() {
        this.markedChests = new ArrayList<>();
        this.hasSelectedIndexer = false;
    }

    public PacketNetworkToolSync(List<BlockPos> markedChests, BlockPos selectedIndexer) {
        this.markedChests = markedChests != null ? markedChests : new ArrayList<>();
        this.selectedIndexer = selectedIndexer;
        this.hasSelectedIndexer = selectedIndexer != null;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int count = buf.readInt();
        markedChests = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            markedChests.add(new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()));
        }

        hasSelectedIndexer = buf.readBoolean();
        if (hasSelectedIndexer) {
            selectedIndexer = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(markedChests.size());
        for (BlockPos pos : markedChests) {
            buf.writeInt(pos.getX());
            buf.writeInt(pos.getY());
            buf.writeInt(pos.getZ());
        }

        buf.writeBoolean(hasSelectedIndexer);
        if (hasSelectedIndexer && selectedIndexer != null) {
            buf.writeInt(selectedIndexer.getX());
            buf.writeInt(selectedIndexer.getY());
            buf.writeInt(selectedIndexer.getZ());
        }
    }

    public static class Handler implements IMessageHandler<PacketNetworkToolSync, IMessage> {
        @Override
        public IMessage onMessage(PacketNetworkToolSync message, MessageContext ctx) {
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
                // Store data in client-side cache
                NetworkToolClientCache.markedChests = new ArrayList<>(message.markedChests);
                NetworkToolClientCache.selectedIndexer = message.selectedIndexer;
            });
            return null;
        }
    }
}
