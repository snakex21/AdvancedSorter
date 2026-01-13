package com.antigravity.advancedsorter.network;

import com.antigravity.advancedsorter.autocrafter.TileAutoCrafter;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Pakiet do kopiowania receptur między Auto Crafterami.
 */
public class PacketCopyAutoCrafterRecipes implements IMessage {

    private BlockPos sourcePos;
    private BlockPos targetPos;

    public PacketCopyAutoCrafterRecipes() {
    }

    public PacketCopyAutoCrafterRecipes(BlockPos source, BlockPos target) {
        this.sourcePos = source;
        this.targetPos = target;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        sourcePos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        targetPos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(sourcePos.getX());
        buf.writeInt(sourcePos.getY());
        buf.writeInt(sourcePos.getZ());
        buf.writeInt(targetPos.getX());
        buf.writeInt(targetPos.getY());
        buf.writeInt(targetPos.getZ());
    }

    public static class Handler implements IMessageHandler<PacketCopyAutoCrafterRecipes, IMessage> {

        @Override
        public IMessage onMessage(PacketCopyAutoCrafterRecipes message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                World world = player.world;

                TileEntity sourceTile = world.getTileEntity(message.sourcePos);
                TileEntity targetTile = world.getTileEntity(message.targetPos);

                if (!(sourceTile instanceof TileAutoCrafter)) {
                    player.sendMessage(new TextComponentString("§cZrodlo nie jest Auto Crafterem!"));
                    return;
                }

                if (!(targetTile instanceof TileAutoCrafter)) {
                    player.sendMessage(new TextComponentString("§cCel nie jest Auto Crafterem!"));
                    return;
                }

                TileAutoCrafter source = (TileAutoCrafter) sourceTile;
                TileAutoCrafter target = (TileAutoCrafter) targetTile;

                int recipeCount = source.getRecipes().size();
                target.copyRecipesFrom(source);

                player.sendMessage(new TextComponentString(
                    "§aSkopiowano " + recipeCount + " receptur(y) do Auto Craftera"));
            });
            return null;
        }
    }
}
