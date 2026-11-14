package com.antigravity.advancedsorter.network;

import com.antigravity.advancedsorter.autocrafter.CraftingRecipe;
import com.antigravity.advancedsorter.machines.ammopress.TileAmmoPress;
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
 * Pakiet sieciowy do obsługi akcji Ammo Press (kopia Auto Craftera).
 */
public class PacketAmmoPressAction implements IMessage {

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

    public PacketAmmoPressAction() {
    }

    private PacketAmmoPressAction(BlockPos pos, int action, int recipeId, int amount, ItemStack result) {
        this.pos = pos;
        this.action = action;
        this.recipeId = recipeId;
        this.amount = amount;
        this.resultStack = result;
    }

    public static PacketAmmoPressAction addRecipe(BlockPos pos) {
        return new PacketAmmoPressAction(pos, ACTION_ADD_RECIPE, 0, 0, ItemStack.EMPTY);
    }

    public static PacketAmmoPressAction removeRecipe(BlockPos pos, int recipeId) {
        return new PacketAmmoPressAction(pos, ACTION_REMOVE_RECIPE, recipeId, 0, ItemStack.EMPTY);
    }

    public static PacketAmmoPressAction clearGrid(BlockPos pos) {
        return new PacketAmmoPressAction(pos, ACTION_CLEAR_GRID, 0, 0, ItemStack.EMPTY);
    }

    public static PacketAmmoPressAction craftRecipe(BlockPos pos, int recipeId, int amount) {
        return new PacketAmmoPressAction(pos, ACTION_CRAFT, recipeId, amount, ItemStack.EMPTY);
    }

    public static PacketAmmoPressAction toggleAutomation(BlockPos pos) {
        return new PacketAmmoPressAction(pos, ACTION_TOGGLE_AUTOMATION, 0, 0, ItemStack.EMPTY);
    }

    public static PacketAmmoPressAction addToAutomation(BlockPos pos, int recipeId) {
        return new PacketAmmoPressAction(pos, ACTION_ADD_TO_AUTOMATION, recipeId, 0, ItemStack.EMPTY);
    }

    public static PacketAmmoPressAction removeFromAutomation(BlockPos pos, int recipeId) {
        return new PacketAmmoPressAction(pos, ACTION_REMOVE_FROM_AUTOMATION, recipeId, 0, ItemStack.EMPTY);
    }

    public static PacketAmmoPressAction toggleMode(BlockPos pos) {
        return new PacketAmmoPressAction(pos, ACTION_TOGGLE_MODE, 0, 0, ItemStack.EMPTY);
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

    public static class Handler implements IMessageHandler<PacketAmmoPressAction, IMessage> {

        @Override
        public IMessage onMessage(PacketAmmoPressAction message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                World world = player.world;
                TileEntity tile = world.getTileEntity(message.pos);

                if (!(tile instanceof TileAmmoPress)) return;
                TileAmmoPress press = (TileAmmoPress) tile;

                switch (message.action) {
                    case ACTION_ADD_RECIPE:
                        CraftingRecipe recipe = press.addRecipeFromGridAndSlot();
                        if (recipe != null) {
                            IItemHandler gridForReturn = press.getRecipeGridHandler();
                            for (int i = 0; i < gridForReturn.getSlots(); i++) {
                                ItemStack stack = gridForReturn.extractItem(i, 64, false);
                                if (!stack.isEmpty()) {
                                    if (!player.inventory.addItemStackToInventory(stack)) {
                                        player.dropItem(stack, false);
                                    }
                                }
                            }
                            IItemHandler resultForReturn = press.getRecipeResultSlotHandler();
                            ItemStack resultReturn = resultForReturn.extractItem(0, 64, false);
                            if (!resultReturn.isEmpty()) {
                                if (!player.inventory.addItemStackToInventory(resultReturn)) {
                                    player.dropItem(resultReturn, false);
                                }
                            }
                            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                                "§aDodano recepturę amunicji: " + recipe.getDisplayName()));
                        } else {
                            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                                "§cPołóż składniki na siatce i wynik w slocie wyniku!"));
                        }
                        break;

                    case ACTION_REMOVE_RECIPE:
                        if (press.removeRecipe(message.recipeId)) {
                            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                                "§cUsunięto recepturę"));
                        }
                        break;

                    case ACTION_CLEAR_GRID:
                        IItemHandler grid = press.getRecipeGridHandler();
                        for (int i = 0; i < grid.getSlots(); i++) {
                            ItemStack stack = grid.extractItem(i, 64, false);
                            if (!stack.isEmpty()) {
                                if (!player.inventory.addItemStackToInventory(stack)) {
                                    player.dropItem(stack, false);
                                }
                            }
                        }
                        IItemHandler resultSlot = press.getRecipeResultSlotHandler();
                        ItemStack resultStack = resultSlot.extractItem(0, 64, false);
                        if (!resultStack.isEmpty()) {
                            if (!player.inventory.addItemStackToInventory(resultStack)) {
                                player.dropItem(resultStack, false);
                            }
                        }
                        player.sendMessage(new net.minecraft.util.text.TextComponentString(
                            "§7Wyczyszczono siatkę"));
                        break;

                    case ACTION_CRAFT:
                        CraftingRecipe recipeToCraft = press.getRecipeById(message.recipeId);
                        if (recipeToCraft != null) {
                            int successCount = 0;
                            for (int i = 0; i < message.amount; i++) {
                                if (press.tryCraft(recipeToCraft)) {
                                    successCount++;
                                } else {
                                    break;
                                }
                            }
                            if (successCount > 0) {
                                player.sendMessage(new net.minecraft.util.text.TextComponentString(
                                    "§aWyprodukowano " + successCount + "x " + recipeToCraft.getDisplayName()));
                            } else {
                                player.sendMessage(new net.minecraft.util.text.TextComponentString(
                                    "§cBrak materiałów!"));
                            }
                        }
                        break;

                    case ACTION_TOGGLE_AUTOMATION:
                        press.setAutomationEnabled(!press.isAutomationEnabled());
                        player.sendMessage(new net.minecraft.util.text.TextComponentString(
                            press.isAutomationEnabled() ? "§aAutomatyzacja WŁĄCZONA" : "§cAutomatyzacja WYŁĄCZONA"));
                        break;

                    case ACTION_ADD_TO_AUTOMATION:
                        press.addToAutomation(message.recipeId);
                        CraftingRecipe addedRecipe = press.getRecipeById(message.recipeId);
                        if (addedRecipe != null) {
                            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                                "§aDodano do auto-produkcji: " + addedRecipe.getDisplayName()));
                        }
                        break;

                    case ACTION_REMOVE_FROM_AUTOMATION:
                        press.removeFromAutomation(message.recipeId);
                        CraftingRecipe removedRecipe = press.getRecipeById(message.recipeId);
                        if (removedRecipe != null) {
                            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                                "§cUsunięto z auto-produkcji: " + removedRecipe.getDisplayName()));
                        }
                        break;

                    case ACTION_SET_RESULT:
                        press.updateRecipePreview();
                        break;

                    case ACTION_TOGGLE_MODE:
                        press.setRoundRobinMode(!press.isRoundRobinMode());
                        player.sendMessage(new net.minecraft.util.text.TextComponentString(
                            press.isRoundRobinMode() ? "§aTryb: Cykliczny (Round-Robin)" : "§eTryb: Priorytetowy"));
                        break;
                }
            });
            return null;
        }
    }
}
