package com.antigravity.advancedsorter.tiles;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import com.antigravity.advancedsorter.network.PacketHandler;
import com.antigravity.advancedsorter.network.PacketInventoryIndexSync;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import java.util.stream.Collectors; // For easy list conversion if needed
import net.minecraft.world.WorldServer;
import net.minecraftforge.items.ItemHandlerHelper; // Utility
import java.util.Iterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TileInventoryIndex extends TileEntity implements ITickable {

    // Individual chests (not in any group) - these are directly linked
    private List<BlockPos> linkedChests = new ArrayList<>();

    // Chest groups - chests in groups are treated as one logical unit
    private List<ChestGroup> chestGroups = new ArrayList<>();

    private int tickCount = 0;

    // Output slots for retrieval (9 slots)
    private ItemStackHandler outputHandler = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }
    };

    public TileInventoryIndex() {
    }

    @Override
    public void update() {
        tickCount++;
        if (tickCount % 20 == 0) { // Scan every second
            scanInventories();
        }
    }

    public void addLinkedChest(BlockPos pos) {
        // Check if chest is already in a group
        if (isChestInAnyGroup(pos)) {
            return;
        }
        if (!linkedChests.contains(pos)) {
            linkedChests.add(pos);
            markDirty();
            syncToClients();
        }
    }

    /**
     * Adds multiple chests at once without syncing after each one.
     * Returns the number of chests actually added.
     */
    public int addLinkedChestsBulk(List<BlockPos> positions) {
        int added = 0;
        for (BlockPos pos : positions) {
            if (!isChestInAnyGroup(pos) && !linkedChests.contains(pos)) {
                linkedChests.add(pos);
                added++;
            }
        }
        if (added > 0) {
            markDirty();
            syncToClients();
        }
        return added;
    }

    public void removeLinkedChest(BlockPos pos) {
        if (linkedChests.remove(pos)) {
            markDirty();
            syncToClients();
        }
    }

    // ========== GROUP MANAGEMENT ==========

    /**
     * Creates a new empty chest group
     */
    public ChestGroup createGroup(String name) {
        ChestGroup group = new ChestGroup(name);
        chestGroups.add(group);
        markDirty();
        syncToClients();
        return group;
    }

    /**
     * Removes a group and optionally moves its chests back to individual list
     */
    public void removeGroup(String groupId, boolean keepChests) {
        ChestGroup toRemove = null;
        for (ChestGroup g : chestGroups) {
            if (g.getGroupId().equals(groupId)) {
                toRemove = g;
                break;
            }
        }
        if (toRemove != null) {
            if (keepChests) {
                // Move chests back to individual list
                for (BlockPos pos : toRemove.getChestPositions()) {
                    if (!linkedChests.contains(pos)) {
                        linkedChests.add(pos);
                    }
                }
            }
            chestGroups.remove(toRemove);
            markDirty();
            syncToClients();
        }
    }

    /**
     * Adds a chest to a specific group
     * Removes it from individual list if present
     */
    public void addChestToGroup(BlockPos chestPos, String groupId) {
        for (ChestGroup group : chestGroups) {
            if (group.getGroupId().equals(groupId)) {
                // Remove from individual list if present
                linkedChests.remove(chestPos);
                // Remove from other groups if present
                for (ChestGroup other : chestGroups) {
                    if (!other.getGroupId().equals(groupId)) {
                        other.removeChest(chestPos);
                    }
                }
                // Add to target group
                group.addChest(chestPos);
                markDirty();
                syncToClients();
                return;
            }
        }
    }

    /**
     * Removes a chest from its group
     * Optionally adds it back to individual list
     */
    public void removeChestFromGroup(BlockPos chestPos, boolean addToIndividual) {
        for (ChestGroup group : chestGroups) {
            if (group.containsChest(chestPos)) {
                group.removeChest(chestPos);
                if (addToIndividual && !linkedChests.contains(chestPos)) {
                    linkedChests.add(chestPos);
                }
                markDirty();
                syncToClients();
                return;
            }
        }
    }

    /**
     * Checks if a chest is in any group
     */
    public boolean isChestInAnyGroup(BlockPos pos) {
        for (ChestGroup group : chestGroups) {
            if (group.containsChest(pos)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets all chest groups
     */
    public List<ChestGroup> getChestGroups() {
        return chestGroups;
    }

    /**
     * Gets a group by ID
     */
    public ChestGroup getGroupById(String groupId) {
        for (ChestGroup group : chestGroups) {
            if (group.getGroupId().equals(groupId)) {
                return group;
            }
        }
        return null;
    }

    /**
     * Gets all chest positions (both individual and grouped)
     */
    public List<BlockPos> getAllChestPositions() {
        List<BlockPos> all = new ArrayList<>(linkedChests);
        for (ChestGroup group : chestGroups) {
            for (BlockPos pos : group.getChestPositions()) {
                if (!all.contains(pos)) {
                    all.add(pos);
                }
            }
        }
        return all;
    }

    private void scanInventories() {
        if (world.isRemote)
            return;

        Map<String, PacketInventoryIndexSync.IndexEntry> aggregation = new HashMap<>();

        // Scan all chests (both individual and from groups)
        List<BlockPos> allChests = getAllChestPositions();

        for (BlockPos pos : allChests) {
            if (!world.isBlockLoaded(pos))
                continue;

            TileEntity te = world.getTileEntity(pos);
            if (te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
                IItemHandler handler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        String key = stack.getUnlocalizedName() + ":" + stack.getMetadata(); // Simple key

                        // Update existing or create new
                        if (aggregation.containsKey(key)) {
                            PacketInventoryIndexSync.IndexEntry entry = aggregation.get(key);
                            entry.count += stack.getCount();
                            if (!entry.locations.contains(pos))
                                entry.locations.add(pos);
                        } else {
                            List<BlockPos> locs = new ArrayList<>();
                            locs.add(pos);
                            aggregation.put(key,
                                    new PacketInventoryIndexSync.IndexEntry(stack.copy(), stack.getCount(), locs));
                        }
                    }
                }
            }
        }

        // Convert to List
        List<PacketInventoryIndexSync.IndexEntry> unsorted = new ArrayList<>(aggregation.values());

        // Validation: send packet to players viewing the GUI
        // We can check players in range, or players who have the container open.
        // For simplicity: Broadcast to all in dimension or check container class.
        if (world instanceof WorldServer) {
            for (net.minecraft.entity.player.EntityPlayer player : world.playerEntities) {
                if (player.openContainer instanceof com.antigravity.advancedsorter.container.ContainerInventoryIndex) {
                    // Check if looking at THIS tile? Technically container doesn't link to tile
                    // easily without reflection or field,
                    // but usually we can assume if they are opening this container type, we might
                    // as well sync.
                    // Or better: The container usually has the tile.
                    // A cleaner way: The container listener.
                    // For now: Just send to all in range of block.
                    if (player.getDistanceSq(this.pos) < 64 * 64) {
                        PacketHandler.INSTANCE.sendTo(new PacketInventoryIndexSync(this.pos, unsorted), (EntityPlayerMP) player);
                    }
                }
            }
        }
    }

    /**
     * Attempts to retrieve specific items from linked chests and place them in the
     * output buffer.
     */
    public void requestItem(ItemStack template, int amount) {
        if (amount <= 0 || template.isEmpty())
            return;

        int remainingNeeded = amount;

        // Get all chests (both individual and from groups)
        List<BlockPos> allChests = getAllChestPositions();

        for (BlockPos chestPos : allChests) {
            if (remainingNeeded <= 0)
                break;

            if (!world.isBlockLoaded(chestPos))
                continue;

            TileEntity te = world.getTileEntity(chestPos);
            if (te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
                IItemHandler chestHandler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);

                // Try to extract from this chest
                for (int slot = 0; slot < chestHandler.getSlots(); slot++) {
                    ItemStack inSlot = chestHandler.getStackInSlot(slot);

                    if (isItemEqual(inSlot, template)) {
                        // How much can we take?
                        int extractable = Math.min(inSlot.getCount(), remainingNeeded);

                        // Simulate extraction
                        ItemStack simulatedExtract = chestHandler.extractItem(slot, extractable, true);
                        if (!simulatedExtract.isEmpty()) {
                            // Try to insert into Output Buffer
                            ItemStack remainingAfterInsert = ItemHandlerHelper.insertItem(outputHandler,
                                    simulatedExtract, false); // Actual insert
                            int actuallyInserted = simulatedExtract.getCount() - remainingAfterInsert.getCount();

                            if (actuallyInserted > 0) {
                                // Now actually remove from chest
                                chestHandler.extractItem(slot, actuallyInserted, false);
                                remainingNeeded -= actuallyInserted;
                                markDirty();

                                if (remainingNeeded <= 0)
                                    break;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isItemEqual(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty())
            return false;
        return a.getItem() == b.getItem() && a.getMetadata() == b.getMetadata();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        compound.setTag("OutputItems", outputHandler.serializeNBT());

        // Save individual chests
        NBTTagList chestList = new NBTTagList();
        for (BlockPos pos : linkedChests) {
            chestList.appendTag(NBTUtil.createPosTag(pos));
        }
        compound.setTag("LinkedChests", chestList);

        // Save chest groups
        NBTTagList groupList = new NBTTagList();
        for (ChestGroup group : chestGroups) {
            groupList.appendTag(group.writeToNBT());
        }
        compound.setTag("ChestGroups", groupList);

        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        if (compound.hasKey("OutputItems")) {
            outputHandler.deserializeNBT(compound.getCompoundTag("OutputItems"));
        }

        // Load individual chests
        if (compound.hasKey("LinkedChests")) {
            NBTTagList chestList = compound.getTagList("LinkedChests", 10);
            linkedChests.clear();
            for (int i = 0; i < chestList.tagCount(); i++) {
                linkedChests.add(NBTUtil.getPosFromTag(chestList.getCompoundTagAt(i)));
            }
        }

        // Load chest groups
        if (compound.hasKey("ChestGroups")) {
            NBTTagList groupList = compound.getTagList("ChestGroups", 10);
            chestGroups.clear();
            for (int i = 0; i < groupList.tagCount(); i++) {
                chestGroups.add(ChestGroup.readFromNBT(groupList.getCompoundTagAt(i)));
            }
        }
    }

    public List<BlockPos> getLinkedChests() {
        return linkedChests;
    }

    @Override
    public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> capability,
            @javax.annotation.Nullable net.minecraft.util.EnumFacing facing) {
        if (capability == net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> capability,
            @javax.annotation.Nullable net.minecraft.util.EnumFacing facing) {
        if (capability == net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) outputHandler;
        }
        return super.getCapability(capability, facing);
    }

    // ========== CLIENT-SERVER SYNC ==========

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        writeToNBT(nbt);
        return new SPacketUpdateTileEntity(pos, 1, nbt);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        readFromNBT(tag);
    }

    /**
     * Call this to force sync to all clients
     */
    public void syncToClients() {
        if (world != null && !world.isRemote) {
            markDirty();
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
    }
}
