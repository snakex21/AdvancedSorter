package com.antigravity.advancedsorter.network;

import com.antigravity.advancedsorter.pipes.fluid.teleport.GuiTeleportFluidPipe;
import com.antigravity.advancedsorter.pipes.gas.teleport.GuiTeleportGasPipe;
import com.antigravity.advancedsorter.pipes.teleport.GuiTeleportPipe;
import com.antigravity.advancedsorter.util.TeleportRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

public class PacketTeleportInfo implements IMessage {
    private List<TeleportRegistry.TeleportLocation> locations;

    public PacketTeleportInfo() {
    }

    public PacketTeleportInfo(List<TeleportRegistry.TeleportLocation> locations) {
        this.locations = locations;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        locations = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int dim = buf.readInt();
            BlockPos pos = BlockPos.fromLong(buf.readLong());
            boolean canSend = buf.readBoolean();
            boolean canReceive = buf.readBoolean();
            locations.add(new TeleportRegistry.TeleportLocation(dim, pos, canSend, canReceive));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(locations.size());
        for (TeleportRegistry.TeleportLocation loc : locations) {
            buf.writeInt(loc.dimension);
            buf.writeLong(loc.pos.toLong());
            buf.writeBoolean(loc.canSend);
            buf.writeBoolean(loc.canReceive);
        }
    }

    public static class Handler implements IMessageHandler<PacketTeleportInfo, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketTeleportInfo message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                GuiScreen gui = Minecraft.getMinecraft().currentScreen;
                if (gui instanceof GuiTeleportPipe) {
                    ((GuiTeleportPipe) gui).updateConnectionInfo(message.locations);
                } else if (gui instanceof GuiTeleportFluidPipe) {
                    ((GuiTeleportFluidPipe) gui).updateConnectionInfo(message.locations);
                } else if (gui instanceof GuiTeleportGasPipe) {
                    ((GuiTeleportGasPipe) gui).updateConnectionInfo(message.locations);
                }
            });
            return null;
        }
    }
}
