package com.antigravity.advancedsorter.network;

import com.antigravity.advancedsorter.autocrafter.CraftingRecipe;
import com.antigravity.advancedsorter.autocrafter.TileAutoCrafter;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.items.IItemHandler;

/**
 * Pakiet sieciowy do obsługi akcji Auto Craftera.
 */
public class PacketAutoCrafterAction implements IMessage {

    public static final int ACTION_ADD_RECIPE = 0;
    public static final int ACTION_REMOVE_RECIPE = 1;
    public static final int ACTION_CLEAR_GRID = 2;
    public static final int ACTION_CRAFT = 3;
    public static final int ACTION_TOGGLE_AUTOMATION = 4;
    public static final int ACTION_ADD_TO_AUTOMATION = 5;
    public static final int ACTION_REMOVE_FROM_AUTOMATION = 6;
    public static final int ACTION_SET_RESULT = 7;
    public static final int ACTION_TOGGLE_MODE = 8;

    private BlockPos pos;
    private int action;
    private int recipeId;
    private int amount;
    private ItemStack resultStack = ItemStack.EMPTY;

    public PacketAutoCrafterAction() {
    }

    private PacketAutoCrafterAction(BlockPos pos, int action, int recipeId, int amount, ItemStack result) {
        this.pos = pos;
        this.action = action;
        this.recipeId = recipeId;
        this.amount = amount;
        this.resultStack = result;
    }

    public static PacketAutoCrafterAction addRecipe(BlockPos pos) {
        return new PacketAutoCrafterAction(pos, ACTION_ADD_RECIPE, 0, 0, ItemStack.EMPTY);
    }

    public static PacketAutoCrafterAction addRecipeWithResult(BlockPos pos, ItemStack result) {
        return new PacketAutoCrafterAction(pos, ACTION_ADD_RECIPE, 0, 0, result);
    }

    public static PacketAutoCrafterAction removeRecipe(BlockPos pos, int recipeId) {
        return new PacketAutoCrafterAction(pos, ACTION_REMOVE_RECIPE, recipeId, 0, ItemStack.EMPTY);
    }

    public static PacketAutoCrafterAction clearGrid(BlockPos pos) {
        return new PacketAutoCrafterAction(pos, ACTION_CLEAR_GRID, 0, 0, ItemStack.EMPTY);
    }

    public static PacketAutoCrafterAction craftRecipe(BlockPos pos, int recipeId, int amount) {
        return new PacketAutoCrafterAction(pos, ACTION_CRAFT, recipeId, amount, ItemStack.EMPTY);
    }

    public static PacketAutoCrafterAction toggleAutomation(BlockPos pos) {
        return new PacketAutoCrafterAction(pos, ACTION_TOGGLE_AUTOMATION, 0, 0, ItemStack.EMPTY);
    }

    public static PacketAutoCrafterAction addToAutomation(BlockPos pos, int recipeId) {
        return new PacketAutoCrafterAction(pos, ACTION_ADD_TO_AUTOMATION, recipeId, 0, ItemStack.EMPTY);
    }

    public static PacketAutoCrafterAction removeFromAutomation(BlockPos pos, int recipeId) {
        return new PacketAutoCrafterAction(pos, ACTION_REMOVE_FROM_AUTOMATION, recipeId, 0, ItemStack.EMPTY);
    }

    public static PacketAutoCrafterAction setRecipeResult(BlockPos pos, ItemStack result) {
        return new PacketAutoCrafterAction(pos, ACTION_SET_RESULT, 0, 0, result);
    }

    public static PacketAutoCrafterAction toggleMode(BlockPos pos) {
        return new PacketAutoCrafterAction(pos, ACTION_TOGGLE_MODE, 0, 0, ItemStack.EMPTY);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        action = buf.readInt();
        recipeId = buf.readInt();
        amount = buf.readInt();
        resultStack = ByteBufUtils.readItemStack(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
        buf.writeInt(action);
        buf.writeInt(recipeId);
        buf.writeInt(amount);
        ByteBufUtils.writeItemStack(buf, resultStack);
    }

    public static class Handler implements IMessageHandler<PacketAutoCrafterAction, IMessage> {

        @Override
        public IMessage onMessage(PacketAutoCrafterAction message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                World world = player.world;
                TileEntity tile = world.getTileEntity(message.pos);

                if (!(tile instanceof TileAutoCrafter)) return;
                TileAutoCrafter crafter = (TileAutoCrafter) tile;

                switch (message.action) {
                    case ACTION_ADD_RECIPE:
                        // Dodaj recepture z siatki i slotu wyniku
                        CraftingRecipe recipe = crafter.addRecipeFromGridAndSlot();
                        if (recipe != null) {
                            // Zwroc przedmioty z siatki do gracza
                            IItemHandler gridForReturn = crafter.getRecipeGridHandler();
                            for (int i = 0; i < gridForReturn.getSlots(); i++) {
                                ItemStack stack = gridForReturn.extractItem(i, 64, false);
                                if (!stack.isEmpty()) {
                                    if (!player.inventory.addItemStackToInventory(stack)) {
                                        player.dropItem(stack, false);
                                    }
                                }
                            }
                            // Zwroc przedmiot z slotu wyniku
                            IItemHandler resultForReturn = crafter.getRecipeResultSlotHandler();
                            ItemStack resultReturn = resultForReturn.extractItem(0, 64, false);
                            if (!resultReturn.isEmpty()) {
                                if (!player.inventory.addItemStackToInventory(resultReturn)) {
                                    player.dropItem(resultReturn, false);
                                }
                            }
                            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                                "§aDodano recepture: " + recipe.getDisplayName() + " §7(przedmioty zwrocone)"));
                        } else {
                            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                                "§cPoloz skladniki na siatce i wynik w slocie wyniku!"));
                        }
                        break;

                    case ACTION_REMOVE_RECIPE:
                        if (crafter.removeRecipe(message.recipeId)) {
                            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                                "§cUsunieto recepture"));
                        }
                        break;

                    case ACTION_CLEAR_GRID:
                        // Wyczysc siatke i slot wyniku - przenies przedmioty do inwentarza gracza
                        IItemHandler grid = crafter.getRecipeGridHandler();
                        for (int i = 0; i < grid.getSlots(); i++) {
                            ItemStack stack = grid.extractItem(i, 64, false);
                            if (!stack.isEmpty()) {
                                if (!player.inventory.addItemStackToInventory(stack)) {
                                    player.dropItem(stack, false);
                                }
                            }
                        }
                        // Wyczysc slot wyniku
                        IItemHandler resultSlot = crafter.getRecipeResultSlotHandler();
                        ItemStack resultStack = resultSlot.extractItem(0, 64, false);
                        if (!resultStack.isEmpty()) {
                            if (!player.inventory.addItemStackToInventory(resultStack)) {
                                player.dropItem(resultStack, false);
                            }
                        }
                        player.sendMessage(new net.minecraft.util.text.TextComponentString(
                            "§7Wyczyszczono siatke"));
                        break;

                    case ACTION_CRAFT:
                        CraftingRecipe recipeToCraft = crafter.getRecipeById(message.recipeId);
                        if (recipeToCraft != null) {
                            int successCount = 0;
                            for (int i = 0; i < message.amount; i++) {
                                if (crafter.tryCraft(recipeToCraft)) {
                                    successCount++;
                                } else {
                                    break;
                                }
                            }
                            if (successCount > 0) {
                                player.sendMessage(new net.minecraft.util.text.TextComponentString(
                                    "§aZrobiono " + successCount + "x " + recipeToCraft.getDisplayName()));
                            } else {
                                player.sendMessage(new net.minecraft.util.text.TextComponentString(
                                    "§cBrak skladnikow!"));
                            }
                        }
                        break;

                    case ACTION_TOGGLE_AUTOMATION:
                        crafter.setAutomationEnabled(!crafter.isAutomationEnabled());
                        player.sendMessage(new net.minecraft.util.text.TextComponentString(
                            crafter.isAutomationEnabled() ? "§aAutomatyzacja WLACZONA" : "§cAutomatyzacja WYLACZONA"));
                        break;

                    case ACTION_ADD_TO_AUTOMATION:
                        crafter.addToAutomation(message.recipeId);
                        CraftingRecipe addedRecipe = crafter.getRecipeById(message.recipeId);
                        if (addedRecipe != null) {
                            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                                "§aDodano do automatyzacji: " + addedRecipe.getDisplayName()));
                        }
                        break;

                    case ACTION_REMOVE_FROM_AUTOMATION:
                        crafter.removeFromAutomation(message.recipeId);
                        CraftingRecipe removedRecipe = crafter.getRecipeById(message.recipeId);
                        if (removedRecipe != null) {
                            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                                "§cUsunieto z automatyzacji: " + removedRecipe.getDisplayName()));
                        }
                        break;

                    case ACTION_SET_RESULT:
                        crafter.updateRecipePreview();
                        break;

                    case ACTION_TOGGLE_MODE:
                        crafter.setRoundRobinMode(!crafter.isRoundRobinMode());
                        player.sendMessage(new net.minecraft.util.text.TextComponentString(
                            crafter.isRoundRobinMode() ? "§aTryb: Cykliczny (Round-Robin)" : "§eTryb: Priorytetowy (1->2->3)"));
                        break;
                }
            });
            return null;
        }
    }
}
