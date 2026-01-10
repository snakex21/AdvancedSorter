package com.antigravity.advancedsorter.blocks;

import com.antigravity.advancedsorter.AdvancedSorterMod;
import com.antigravity.advancedsorter.tiles.TileInventoryIndex;
import com.antigravity.advancedsorter.tiles.ChestGroup;
import com.antigravity.advancedsorter.network.PacketHandler;
import com.antigravity.advancedsorter.network.PacketChestGroupSync;
import com.antigravity.advancedsorter.network.PacketNetworkToolSync;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;

import java.util.ArrayList;
import java.util.List;

public class ItemNetworkTool extends Item {

    public ItemNetworkTool() {
        setUnlocalizedName("network_tool");
        setRegistryName("network_tool");
        setCreativeTab(AdvancedSorterMod.CREATIVE_TAB);
        setMaxStackSize(1);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand,
            EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote)
            return EnumActionResult.SUCCESS;

        ItemStack stack = player.getHeldItem(hand);
        TileEntity tile = worldIn.getTileEntity(pos);
        boolean isSneaking = player.isSneaking();

        BlockPos copySource = getCopySourceIndexer(stack);
        List<BlockPos> markedChests = getMarkedChests(stack);

        // ==================== COPY MODE ====================
        // When we have a copy source set, we're in "copy mode"
        // Only allow: paste to another indexer, or cancel with shift+click on source
        if (copySource != null) {
            if (tile instanceof TileInventoryIndex) {
                TileInventoryIndex indexer = (TileInventoryIndex) tile;

                if (copySource.equals(pos)) {
                    // Clicking on the same indexer - cancel copy mode
                    if (isSneaking) {
                        clearCopySourceIndexer(stack);
                        syncToClient(player, stack);
                        player.sendMessage(new TextComponentString(
                                "§cCancelled copy mode."));
                    } else {
                        player.sendMessage(new TextComponentString(
                                "§eThis is the source Indexer. Shift+Click to cancel, or click another Indexer to paste."));
                    }
                    return EnumActionResult.SUCCESS;
                }

                // Different indexer - paste connections
                TileEntity sourceTile = worldIn.getTileEntity(copySource);
                if (sourceTile instanceof TileInventoryIndex) {
                    TileInventoryIndex sourceIndexer = (TileInventoryIndex) sourceTile;

                    // Collect all chests to copy
                    List<BlockPos> chestsToCopy = new ArrayList<>();

                    // Add all individual chests
                    for (BlockPos chestPos : sourceIndexer.getLinkedChests()) {
                        if (!chestsToCopy.contains(chestPos)) {
                            chestsToCopy.add(chestPos);
                        }
                    }

                    // Add all chests from groups
                    for (ChestGroup group : sourceIndexer.getChestGroups()) {
                        for (BlockPos chestPos : group.getChestPositions()) {
                            if (!chestsToCopy.contains(chestPos)) {
                                chestsToCopy.add(chestPos);
                            }
                        }
                    }

                    // Add all chests in bulk
                    int added = indexer.addLinkedChestsBulk(chestsToCopy);

                    if (added > 0) {
                        player.sendMessage(new TextComponentString(
                                "§aPasted " + added + " chest connection(s) to this Indexer."));
                    } else {
                        player.sendMessage(new TextComponentString(
                                "§eAll chests are already linked to this Indexer."));
                    }

                    // Clear copy source after pasting
                    clearCopySourceIndexer(stack);
                    setSelectedIndexer(stack, pos);
                    syncToClient(player, stack);
                } else {
                    // Source indexer no longer exists
                    clearCopySourceIndexer(stack);
                    player.sendMessage(new TextComponentString(
                            "§cSource Indexer no longer exists. Copy mode cancelled."));
                    syncToClient(player, stack);
                }
                return EnumActionResult.SUCCESS;
            }

            // Clicked on something else while in copy mode - show reminder
            player.sendMessage(new TextComponentString(
                    "§eYou are in copy mode. Click another Indexer to paste, or Shift+Click source Indexer to cancel."));
            return EnumActionResult.SUCCESS;
        }

        // ==================== NORMAL MODE ====================
        // No copy source set - normal operation

        // ========== CLICK ON INVENTORY INDEXER ==========
        if (tile instanceof TileInventoryIndex) {
            TileInventoryIndex indexer = (TileInventoryIndex) tile;

            if (isSneaking && markedChests.isEmpty()) {
                // Shift+Click on Indexer with no marked chests = enter copy mode
                setCopySourceIndexer(stack, pos);
                syncToClient(player, stack);

                int totalChests = indexer.getLinkedChests().size();
                for (ChestGroup group : indexer.getChestGroups()) {
                    totalChests += group.size();
                }

                player.sendMessage(new TextComponentString(
                        "§aCopied Inventory Indexer §7(" + totalChests + " chests)\n" +
                        "§7Click another Indexer to paste. Shift+Click here to cancel."));
                return EnumActionResult.SUCCESS;
            }

            if (!markedChests.isEmpty()) {
                // We have marked chests - add them to this indexer
                int added = indexer.addLinkedChestsBulk(markedChests);

                if (added > 0) {
                    player.sendMessage(new TextComponentString(
                            "§aAdded " + added + " chest(s) to Indexer."));
                } else {
                    player.sendMessage(new TextComponentString(
                            "§eAll chests are already linked to this Indexer."));
                }

                // Clear marked chests after adding
                clearMarkedChests(stack);
                setSelectedIndexer(stack, pos);
                syncToClient(player, stack);
                return EnumActionResult.SUCCESS;
            }

            // No marked chests, not sneaking - just select this Indexer
            setSelectedIndexer(stack, pos);
            syncToClient(player, stack);

            int totalChests = indexer.getLinkedChests().size();
            for (ChestGroup group : indexer.getChestGroups()) {
                totalChests += group.size();
            }

            player.sendMessage(new TextComponentString(
                    "§aSelected Indexer at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() +
                    " §7(" + totalChests + " chests linked)\n" +
                    "§7Right-click in air to manage chests."));
            return EnumActionResult.SUCCESS;
        }

        // ========== CLICK ON CHEST/INVENTORY ==========
        if (tile != null && tile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
            if (isSneaking) {
                // Shift+Click = Mark/Unmark chest
                if (markedChests.contains(pos)) {
                    // Already marked - unmark it
                    markedChests.remove(pos);
                    setMarkedChests(stack, markedChests);
                    syncToClient(player, stack);
                    player.sendMessage(new TextComponentString(
                            "§cUnmarked chest at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() +
                            " §7(" + markedChests.size() + " marked)"));
                } else {
                    // Mark it
                    markedChests.add(pos);
                    setMarkedChests(stack, markedChests);
                    syncToClient(player, stack);
                    player.sendMessage(new TextComponentString(
                            "§aMarked chest at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() +
                            " §7(" + markedChests.size() + " marked)"));
                }
            } else {
                // Normal click on chest - check if it's already linked and offer to remove
                BlockPos selectedIndexerPos = getSelectedIndexer(stack);
                if (selectedIndexerPos != null) {
                    TileEntity indexerTile = worldIn.getTileEntity(selectedIndexerPos);
                    if (indexerTile instanceof TileInventoryIndex) {
                        TileInventoryIndex indexer = (TileInventoryIndex) indexerTile;

                        // Check if chest is linked to this indexer
                        if (indexer.getLinkedChests().contains(pos) || indexer.isChestInAnyGroup(pos)) {
                            // Remove it
                            if (indexer.isChestInAnyGroup(pos)) {
                                indexer.removeChestFromGroup(pos, false);
                                player.sendMessage(new TextComponentString(
                                        "§cRemoved chest from group."));
                            } else {
                                indexer.removeLinkedChest(pos);
                                player.sendMessage(new TextComponentString(
                                        "§cRemoved chest from Indexer."));
                            }
                            return EnumActionResult.SUCCESS;
                        }
                    }
                }

                // Not linked - just mark it (same as shift click for convenience)
                if (!markedChests.contains(pos)) {
                    markedChests.add(pos);
                    setMarkedChests(stack, markedChests);
                    syncToClient(player, stack);
                    player.sendMessage(new TextComponentString(
                            "§aMarked chest at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() +
                            " §7(" + markedChests.size() + " marked)"));
                }
            }
            return EnumActionResult.SUCCESS;
        }

        return EnumActionResult.PASS;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (worldIn.isRemote) {
            // Open GUI on client side - use client cache data
            List<BlockPos> markedChests = com.antigravity.advancedsorter.network.NetworkToolClientCache.markedChests;
            BlockPos selectedIndexer = com.antigravity.advancedsorter.network.NetworkToolClientCache.selectedIndexer;

            if (selectedIndexer != null || !markedChests.isEmpty()) {
                net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(
                    new com.antigravity.advancedsorter.client.gui.GuiNetworkTool(player));
            }
        } else {
            // Server side - just show help if nothing selected
            List<BlockPos> markedChests = getMarkedChests(stack);
            BlockPos selectedIndexer = getSelectedIndexer(stack);

            if (selectedIndexer == null && markedChests.isEmpty()) {
                player.sendMessage(new TextComponentString(
                        "§7Network Tool:\n" +
                        "§e- Click on Indexer to select it\n" +
                        "§e- Shift+Click on chests to mark them\n" +
                        "§e- Shift+Click on Indexer to copy its connections\n" +
                        "§e- Right-click in air to open manager"));
            }
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    // ========== NBT HELPERS ==========

    public static List<BlockPos> getMarkedChests(ItemStack stack) {
        List<BlockPos> positions = new ArrayList<>();
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey("MarkedChests")) {
            NBTTagList list = tag.getTagList("MarkedChests", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                positions.add(NBTUtil.getPosFromTag(list.getCompoundTagAt(i)));
            }
        }
        return positions;
    }

    public static void setMarkedChests(ItemStack stack, List<BlockPos> positions) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) tag = new NBTTagCompound();

        NBTTagList list = new NBTTagList();
        for (BlockPos pos : positions) {
            list.appendTag(NBTUtil.createPosTag(pos));
        }
        tag.setTag("MarkedChests", list);
        stack.setTagCompound(tag);
    }

    public static void clearMarkedChests(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            tag.removeTag("MarkedChests");
            stack.setTagCompound(tag);
        }
    }

    public static BlockPos getSelectedIndexer(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey("SelectedIndexer")) {
            return NBTUtil.getPosFromTag(tag.getCompoundTag("SelectedIndexer"));
        }
        return null;
    }

    public static void setSelectedIndexer(ItemStack stack, BlockPos pos) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) tag = new NBTTagCompound();
        tag.setTag("SelectedIndexer", NBTUtil.createPosTag(pos));
        stack.setTagCompound(tag);
    }

    public static void clearSelectedIndexer(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            tag.removeTag("SelectedIndexer");
            stack.setTagCompound(tag);
        }
    }

    // ========== COPY SOURCE INDEXER (for copying connections between indexers) ==========

    public static BlockPos getCopySourceIndexer(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey("CopySourceIndexer")) {
            return NBTUtil.getPosFromTag(tag.getCompoundTag("CopySourceIndexer"));
        }
        return null;
    }

    public static void setCopySourceIndexer(ItemStack stack, BlockPos pos) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) tag = new NBTTagCompound();
        tag.setTag("CopySourceIndexer", NBTUtil.createPosTag(pos));
        stack.setTagCompound(tag);
    }

    public static void clearCopySourceIndexer(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            tag.removeTag("CopySourceIndexer");
            stack.setTagCompound(tag);
        }
    }

    /**
     * Syncs the Network Tool data to the client
     */
    public static void syncToClient(EntityPlayer player, ItemStack stack) {
        if (player instanceof EntityPlayerMP) {
            List<BlockPos> marked = getMarkedChests(stack);
            BlockPos selected = getSelectedIndexer(stack);
            PacketHandler.INSTANCE.sendTo(new PacketNetworkToolSync(marked, selected), (EntityPlayerMP) player);
        }
    }
}
