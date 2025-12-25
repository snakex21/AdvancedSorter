package com.antigravity.advancedsorter.network;

import com.antigravity.advancedsorter.pump.TilePumpController;
import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet to update pump controller presets.
 */
public class PacketUpdatePumpController implements IMessage {

    private BlockPos pos;
    private List<TilePumpController.PumpPreset> presets;

    public PacketUpdatePumpController() {
    }

    public PacketUpdatePumpController(BlockPos pos, List<TilePumpController.PumpPreset> presets) {
        this.pos = pos;
        this.presets = presets;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        int size = buf.readInt();
        presets = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String name = ByteBufUtils.readUTF8String(buf);
            int frequency = buf.readInt();
            boolean enabled = buf.readBoolean();
            presets.add(new TilePumpController.PumpPreset(name, frequency, enabled));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeInt(presets.size());
        for (TilePumpController.PumpPreset preset : presets) {
            ByteBufUtils.writeUTF8String(buf, preset.name);
            buf.writeInt(preset.frequency);
            buf.writeBoolean(preset.enabled);
        }
    }

    public static class Handler implements IMessageHandler<PacketUpdatePumpController, IMessage> {
        @Override
        public IMessage onMessage(PacketUpdatePumpController message, MessageContext ctx) {
            ctx.getServerHandler().player.getServerWorld().addScheduledTask(() -> {
                net.minecraft.world.World world = ctx.getServerHandler().player.world;
                TileEntity te = world.getTileEntity(message.pos);
                if (te instanceof TilePumpController) {
                    TilePumpController controller = (TilePumpController) te;
                    controller.setPresets(message.presets);
                }
            });
            return null;
        }
    }
}
