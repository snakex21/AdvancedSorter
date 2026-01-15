package com.antigravity.advancedsorter.pipes;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Tile entity for item transport pipes.
 * Handles item movement, connections, and loop support.
 */
public class TileItemPipe extends TileEntity implements ITickable {

    // Pipe tier (determines speed)
    protected PipeTier tier = PipeTier.IRON; // Default to iron for backward compatibility

    // Connection state per face (true = connected)
    protected EnumSet<EnumFacing> connections = EnumSet.noneOf(EnumFacing.class);

    // Blocked connections (manually disconnected by wrench - persists and prevents
    // auto-connect)
    protected EnumSet<EnumFacing> blockedConnections = EnumSet.noneOf(EnumFacing.class);

    // Items currently travelling through this pipe
    protected List<TravellingItem> travellingItems = new ArrayList<>();

    // Speed: ticks per block (lower = faster) - now derived from tier
    protected float speed = 10.0f;

    // Round-robin index for output selection (BuildCraft-style)
    protected int roundRobinIndex = 0;

    // Cached valid outputs (updated when connections change)
    protected boolean connectionsDirty = true;

    public TileItemPipe() {
        this(PipeTier.IRON);
    }

    public TileItemPipe(PipeTier tier) {
        this.tier = tier;
        this.speed = tier.getSpeed();
    }

    public PipeTier getTier() {
        return tier;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (world != null && !world.isRemote) {
            // Update our connections
            updateConnections();

            // Notify all neighbor pipes to update their connections
            for (EnumFacing face : EnumFacing.VALUES) {
                BlockPos neighborPos = pos.offset(face);
                TileEntity neighbor = world.getTileEntity(neighborPos);
                if (neighbor instanceof TileItemPipe) {
                    ((TileItemPipe) neighbor).updateConnections();
                }
            }
        }
    }

    @Override
    public void update() {
        if (world == null)
            return;

        // CLIENT SIDE: Interpolation only to smooth out movement between packets
        if (world.isRemote) {
            for (TravellingItem item : travellingItems) {
                item.update(speed);
            }
            return;
        }

        // SERVER SIDE
        boolean stateChanged = false;

        // Update connections if needed
        if (connectionsDirty) {
            updateConnections();
            connectionsDirty = false;
        }

        Iterator<TravellingItem> iterator = travellingItems.iterator();
        while (iterator.hasNext()) {
            TravellingItem item = iterator.next();

            // Calculate direction at center if not set
            if (item.direction == null && item.progress >= 0.5f) {
                item.direction = chooseOutputDirection(item);
                stateChanged = true; // Sync direction choice to client
            }

            // Update position
            boolean reachedEnd = item.update(speed);

            if (reachedEnd) {
                // Item reached end of pipe
                if (item.direction != null) {
                    boolean transferred = transferToNeighbor(item);

                    if (transferred) {
                        iterator.remove();
                        stateChanged = true; // Item removed, sync needed
                    } else {
                        // Failed to transfer - item enters loop mode
                        EnumFacing oldDir = item.direction;
                        item.direction = chooseOutputDirectionExcluding(item, oldDir);
                        item.progress = 0.0f;
                        item.source = oldDir.getOpposite();

                        stateChanged = true; // Sync bounce/retry

                        if (item.direction == null) {
                            // No valid direction - bounce back instead of dropping
                            item.direction = item.source;
                            item.source = oldDir;
                            // If still no direction (single ended pipe), just wait
                            if (item.direction == null) {
                                item.progress = 0.0f; // Reset and wait
                            }
                        }
                    }
                } else {
                    // No valid direction at center - try again next tick
                    item.progress = 0.0f;
                    stateChanged = true; // Sync reset
                }
            }
        }

        // Only sync if state explicitly changed (item entered/left/turned/bounced)
        // This drastically reduces packet spam compared to syncing every tick
        if (stateChanged) {
            markDirty();
            sendUpdate();
        }
    }

    private boolean reachedEndAny = false; // Deprecated, kept for structure but unused in new logic

    /**
     * Choose output direction for an item using BuildCraft-style round-robin.
     * Prioritizes inventory outputs over pipe outputs.
     * Excludes source direction to prevent backtracking.
     */
    protected EnumFacing chooseOutputDirection(TravellingItem item) {
        List<EnumFacing> inventoryOutputs = new ArrayList<>();
        List<EnumFacing> pipeOutputs = new ArrayList<>();

        for (EnumFacing face : connections) {
            // Exclude source (no backtracking)
            if (face == item.source)
                continue;

            // Check if neighbor can receive
            if (canOutputTo(face)) {
                BlockPos neighborPos = pos.offset(face);
                TileEntity neighbor = world.getTileEntity(neighborPos);

                // Separate inventories from pipes (prioritize inventories)
                if (neighbor instanceof TileItemPipe) {
                    pipeOutputs.add(face);
                } else {
                    inventoryOutputs.add(face);
                }
            }
        }

        // Prioritize inventory outputs over pipe outputs
        List<EnumFacing> validOutputs = inventoryOutputs.isEmpty() ? pipeOutputs : inventoryOutputs;

        if (validOutputs.isEmpty()) {
            return null;
        }

        // Single output - no need for round-robin
        if (validOutputs.size() == 1) {
            return validOutputs.get(0);
        }

        // Round-robin: cycle through outputs in order
        roundRobinIndex = roundRobinIndex % validOutputs.size();
        EnumFacing selected = validOutputs.get(roundRobinIndex);
        roundRobinIndex = (roundRobinIndex + 1) % validOutputs.size();

        markDirty(); // Save round-robin state
        return selected;
    }

    /**
     * Choose output excluding a specific direction (for loop retry).
     * Uses round-robin like the main method.
     */
    protected EnumFacing chooseOutputDirectionExcluding(TravellingItem item, EnumFacing exclude) {
        List<EnumFacing> inventoryOutputs = new ArrayList<>();
        List<EnumFacing> pipeOutputs = new ArrayList<>();

        for (EnumFacing face : connections) {
            if (face == item.source || face == exclude)
                continue;
            if (canOutputTo(face)) {
                BlockPos neighborPos = pos.offset(face);
                TileEntity neighbor = world.getTileEntity(neighborPos);

                if (neighbor instanceof TileItemPipe) {
                    pipeOutputs.add(face);
                } else {
                    inventoryOutputs.add(face);
                }
            }
        }

        List<EnumFacing> validOutputs = inventoryOutputs.isEmpty() ? pipeOutputs : inventoryOutputs;

        if (validOutputs.isEmpty()) {
            return null;
        }

        if (validOutputs.size() == 1) {
            return validOutputs.get(0);
        }

        // Round-robin for retry as well
        roundRobinIndex = roundRobinIndex % validOutputs.size();
        EnumFacing selected = validOutputs.get(roundRobinIndex);
        roundRobinIndex = (roundRobinIndex + 1) % validOutputs.size();

        return selected;
    }

    /**
     * Check if we can output to a face.
     */
    protected boolean canOutputTo(EnumFacing face) {
        BlockPos neighborPos = pos.offset(face);
        TileEntity neighbor = world.getTileEntity(neighborPos);

        if (neighbor == null)
            return false;

        // Can output to other pipes
        if (neighbor instanceof TileItemPipe) {
            return true;
        }

        // Can output to inventories
        EnumFacing into = face.getOpposite();
        if (neighbor.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, into)) {
            return true;
        }
        if (neighbor.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
            return true;
        }

        return false;
    }

    /**
     * Transfer item to neighbor pipe or inventory.
     * 
     * @return true if transfer successful
     */
    protected boolean transferToNeighbor(TravellingItem item) {
        BlockPos neighborPos = pos.offset(item.direction);
        TileEntity neighbor = world.getTileEntity(neighborPos);

        if (neighbor == null) {
            return false;
        }


        // Transfer to another pipe
        if (neighbor instanceof TileItemPipe) {
            TileItemPipe pipe = (TileItemPipe) neighbor;
            EnumFacing fromDirection = item.direction.getOpposite();

            // Check if neighbor pipe will accept the item
            if (!pipe.canReceiveItem(item.stack, fromDirection)) {
                return false; // Item stays in this pipe
            }

            pipe.receiveItem(item.stack, fromDirection);
            return true;
        }

        // Transfer to inventory - try multiple faces for sided inventories (like
        // furnaces)
        EnumFacing primaryFace = item.direction.getOpposite();

        // Priority order: primary face, UP (for furnace input), null (all slots), then
        // other faces
        EnumFacing[] facesToTry = new EnumFacing[] {
                primaryFace,
                EnumFacing.UP, // Furnace input is from top
                null, // All slots
                EnumFacing.DOWN,
                EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST
        };

        for (EnumFacing face : facesToTry) {
            // Skip primary face if we've already tried it (it's first in list)
            if (face == primaryFace && face != facesToTry[0])
                continue;

            if (neighbor.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face)) {
                IItemHandler handler = neighbor.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face);
                if (handler != null && handler.getSlots() > 0) {
                    ItemStack remaining = ItemHandlerHelper.insertItem(handler, item.stack, false);
                    if (remaining.isEmpty()) {
                        return true;
                    } else if (remaining.getCount() < item.stack.getCount()) {
                        item.stack = remaining;
                        return false;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Check if this pipe can receive an item from the given direction.
     * Override in subclasses to add restrictions (e.g., DirectionalPipe INPUT
     * check).
     */
    public boolean canReceiveItem(ItemStack stack, EnumFacing from) {
        return true; // By default, all pipes accept items from any direction
    }

    public void receiveItem(ItemStack stack, EnumFacing from) {
        receiveItem(stack, from, false);
    }

    public void receiveItem(ItemStack stack, EnumFacing from, boolean teleported) {
        if (stack.isEmpty())
            return;
        TravellingItem item = new TravellingItem(stack.copy(), from);
        item.teleported = teleported;
        travellingItems.add(item);
        markDirty();
        sendUpdate();
    }

    /**
     * Drop item as entity when no valid output.
     */
    protected void dropItem(TravellingItem item) {
        if (!world.isRemote) {
            net.minecraft.entity.item.EntityItem entityItem = new net.minecraft.entity.item.EntityItem(
                    world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, item.stack);
            world.spawnEntity(entityItem);
        }
    }

    /**
     * Update connections based on neighbors.
     * Respects blockedConnections - blocked sides will not auto-connect.
     */
    public void updateConnections() {
        connections.clear();

        for (EnumFacing face : EnumFacing.VALUES) {
            // Skip blocked connections - they never auto-connect
            if (blockedConnections.contains(face)) {
                continue;
            }

            if (canConnectTo(face)) {
                // Also check if neighbor has blocked us
                BlockPos neighborPos = pos.offset(face);
                TileEntity neighbor = world.getTileEntity(neighborPos);
                if (neighbor instanceof TileItemPipe) {
                    TileItemPipe neighborPipe = (TileItemPipe) neighbor;
                    if (neighborPipe.blockedConnections.contains(face.getOpposite())) {
                        continue; // Neighbor blocked connection to us
                    }
                }
                connections.add(face);
            }
        }

        markDirty();
        sendUpdate();
    }

    /**
     * Check if we can connect to a neighbor on this face.
     */
    protected boolean canConnectTo(EnumFacing face) {
        BlockPos neighborPos = pos.offset(face);
        TileEntity neighbor = world.getTileEntity(neighborPos);

        if (neighbor == null)
            return false;

        // Connect to other pipes
        if (neighbor instanceof TileItemPipe) {
            return true;
        }

        // Connect to inventories
        if (neighbor.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face.getOpposite())) {
            return true;
        }
        if (neighbor.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
            return true;
        }

        return false;
    }

    /**
     * Toggle connection on a face (wrench action).
     * Disconnecting blocks the connection on BOTH sides.
     * Reconnecting unblocks and re-establishes connection.
     */
    public void toggleConnection(EnumFacing face) {
        BlockPos neighborPos = pos.offset(face);
        TileEntity neighbor = world.getTileEntity(neighborPos);
        TileItemPipe neighborPipe = (neighbor instanceof TileItemPipe) ? (TileItemPipe) neighbor : null;

        if (connections.contains(face)) {
            // Currently connected - DISCONNECT (block both sides)
            connections.remove(face);
            blockedConnections.add(face);

            // Also block on neighbor side
            if (neighborPipe != null) {
                neighborPipe.connections.remove(face.getOpposite());
                neighborPipe.blockedConnections.add(face.getOpposite());
                neighborPipe.markDirty();
                neighborPipe.sendUpdate();
            }
        } else if (blockedConnections.contains(face)) {
            // Currently blocked - UNBLOCK and reconnect if possible
            blockedConnections.remove(face);

            // Also unblock on neighbor side
            if (neighborPipe != null) {
                neighborPipe.blockedConnections.remove(face.getOpposite());
                neighborPipe.markDirty();
            }

            // Try to reconnect
            if (canConnectTo(face)) {
                connections.add(face);
                if (neighborPipe != null) {
                    neighborPipe.connections.add(face.getOpposite());
                    neighborPipe.sendUpdate();
                }
            }
        } else {
        }

        markDirty();
        sendUpdate();
    }

    /**
     * Send visual update to clients when connections change.
     */
    protected void sendUpdate() {
        if (world != null && !world.isRemote) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    /**
     * Mark connections as needing update.
     */
    public void markConnectionsDirty() {
        connectionsDirty = true;
    }

    // Getters for rendering
    public EnumSet<EnumFacing> getConnections() {
        return connections;
    }

    public List<TravellingItem> getTravellingItems() {
        return travellingItems;
    }

    public float getSpeed() {
        return speed;
    }

    // ========== NBT ==========

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        connections.clear();
        if (compound.hasKey("Connections")) {
            int conn = compound.getInteger("Connections");
            for (EnumFacing face : EnumFacing.VALUES) {
                if ((conn & (1 << face.getIndex())) != 0) {
                    connections.add(face);
                }
            }
        }

        blockedConnections.clear();
        if (compound.hasKey("BlockedConnections")) {
            int blocked = compound.getInteger("BlockedConnections");
            for (EnumFacing face : EnumFacing.VALUES) {
                if ((blocked & (1 << face.getIndex())) != 0) {
                    blockedConnections.add(face);
                }
            }
        }

        travellingItems.clear();
        if (compound.hasKey("Items")) {
            NBTTagList list = compound.getTagList("Items", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < list.tagCount(); i++) {
                travellingItems.add(new TravellingItem(list.getCompoundTagAt(i)));
            }
        }

        speed = compound.getFloat("Speed");
        if (speed <= 0)
            speed = 10.0f;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        int conn = 0;
        for (EnumFacing face : connections) {
            conn |= (1 << face.getIndex());
        }
        compound.setInteger("Connections", conn);

        int blocked = 0;
        for (EnumFacing face : blockedConnections) {
            blocked |= (1 << face.getIndex());
        }
        compound.setInteger("BlockedConnections", blocked);

        NBTTagList list = new NBTTagList();
        for (TravellingItem item : travellingItems) {
            list.appendTag(item.writeToNBT());
        }
        compound.setTag("Items", list);

        compound.setFloat("Speed", speed);

        return compound;
    }

    // ========== Sync ==========

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 1, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        // Save state before update to detect if we need a render update
        EnumSet<EnumFacing> oldConnections = connections.clone();
        EnumSet<EnumFacing> oldBlocked = blockedConnections.clone();

        readFromNBT(pkt.getNbtCompound());

        // Only mark for render update if the connections actually changed.
        // This restores visual updates for wrench/placing while keeping the LAG FIX (ignoring item updates).
        if (world != null && world.isRemote) {
            if (!connections.equals(oldConnections) || !blockedConnections.equals(oldBlocked)) {
                world.markBlockRangeForRenderUpdate(pos, pos);
            }
        }
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        readFromNBT(tag);
    }

    // ========== Capabilities ==========

    // Per-face item handlers that track source direction
    private final java.util.Map<EnumFacing, IItemHandler> facingHandlers = new java.util.EnumMap<>(EnumFacing.class);

    {
        // Initialize handler for each face
        for (EnumFacing face : EnumFacing.VALUES) {
            final EnumFacing sourceFace = face;
            facingHandlers.put(face, new IItemHandler() {
                @Override
                public int getSlots() {
                    return 1;
                }

                @Override
                public ItemStack getStackInSlot(int slot) {
                    return ItemStack.EMPTY;
                }

                @Override
                public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                    if (stack.isEmpty())
                        return ItemStack.EMPTY;

                    if (!simulate) {
                        receiveItem(stack.copy(), sourceFace); // Track source direction!
                    }
                    return ItemStack.EMPTY;
                }

                @Override
                public ItemStack extractItem(int slot, int amount, boolean simulate) {
                    return ItemStack.EMPTY;
                }

                @Override
                public int getSlotLimit(int slot) {
                    return 64;
                }
            });
        }
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true; // Pipes accept items from any side
        }
        return super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            // Return face-specific handler, or any handler for null facing
            if (facing != null) {
                return (T) facingHandlers.get(facing);
            }
            return (T) facingHandlers.get(EnumFacing.UP); // Default for null
        }
        return super.getCapability(capability, facing);
    }
}
