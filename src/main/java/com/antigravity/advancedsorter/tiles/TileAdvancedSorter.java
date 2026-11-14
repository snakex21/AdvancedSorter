package com.antigravity.advancedsorter.tiles;

import com.antigravity.advancedsorter.util.RuleSerializer;
import com.antigravity.advancedsorter.util.SortRule;
import buildcraft.api.transport.IInjectable;
import buildcraft.api.core.EnumColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class TileAdvancedSorter extends TileEntity implements ITickable {

    // Internal buffer: 9 slots
    private final ItemStackHandler inventory = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }
    };

    public List<SortRule> rules = new ArrayList<>();
    public EnumFacing defaultSide = EnumFacing.DOWN;
    public SortRule.DistributionMode distributionMode = SortRule.DistributionMode.FIRST_MATCH;
    private int roundRobinIndex = 0; // Used for Round Robin distribution

    // OPTYMALIZACJA: Cache IItemHandler dla każdej strony
    private final Map<EnumFacing, IItemHandler> neighborHandlerCache = new EnumMap<>(EnumFacing.class);
    // OPTYMALIZACJA: Cache IInjectable (BuildCraft)
    private final Map<EnumFacing, IInjectable> neighborInjectableCache = new EnumMap<>(EnumFacing.class);
    private boolean neighborCacheDirty = true;

    // OPTYMALIZACJA: Reużywalne listy
    private final List<SortRule> reusableMatchingRules = new ArrayList<>();
    private final List<SortRule> reusableRoundRobinRules = new ArrayList<>();

    // OPTYMALIZACJA: Sleep timer
    private int sleepTimer = 0;

    public TileAdvancedSorter() {
    }

    // Wywoływane gdy sąsiad się zmieni
    public void onNeighborChanged() {
        neighborCacheDirty = true;
        sleepTimer = 0; // Wake up
    }

    private void updateNeighborCache() {
        if (!neighborCacheDirty) return;
        neighborCacheDirty = false;
        neighborHandlerCache.clear();
        neighborInjectableCache.clear();

        for (EnumFacing side : EnumFacing.VALUES) {
            BlockPos targetPos = pos.offset(side);
            TileEntity neighbor = world.getTileEntity(targetPos);
            if (neighbor != null) {
                // Capability Cache
                EnumFacing into = side.getOpposite();
                IItemHandler handler = null;
                if (neighbor.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, into)) {
                    handler = neighbor.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, into);
                } else if (neighbor.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
                    handler = neighbor.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
                }
                if (handler != null) {
                    neighborHandlerCache.put(side, handler);
                }

                // BuildCraft Cache
                if (neighbor instanceof IInjectable) {
                    neighborInjectableCache.put(side, (IInjectable) neighbor);
                }
            }
        }
    }

    @Override
    public void update() {
        if (world.isRemote)
            return;

        if (sleepTimer > 0) {
            sleepTimer--;
            return;
        }

        // OPTYMALIZACJA: Aktualizuj cache sąsiadów jeśli potrzeba
        updateNeighborCache();

        boolean hasItems = false;
        boolean didWork = false;

        // Try to eject items from buffer
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                hasItems = true;
                EnumFacing targetSide = getTargetSide(stack);

                ItemStack remaining = pushItemToSide(stack, targetSide);

                if (remaining.getCount() != stack.getCount()) {
                    inventory.setStackInSlot(i, remaining);
                    didWork = true;
                }
            }
        }

        if (!hasItems) {
            sleepTimer = 20; // Sleep 1 second if empty
        } else if (!didWork) {
            sleepTimer = 10; // Sleep 0.5 second if stuck
        }
    }

    private EnumFacing getTargetSide(ItemStack stack) {
        // OPTYMALIZACJA: Reużywaj listy
        reusableMatchingRules.clear();

        for (SortRule rule : rules) {

            if (rule.matches(stack)) {

                // Get effective mode for this rule (global + override)
                SortRule.DistributionMode effectiveMode = getEffectiveMode(rule);

                // If this rule forces FIRST_MATCH, return immediately
                if (effectiveMode == SortRule.DistributionMode.FIRST_MATCH) {
                    return rule.outputFace;
                }

                // Otherwise collect for potential Round Robin
                reusableMatchingRules.add(rule);
            } else {
            }
        }

        // ROUND_ROBIN: cycle through matching rules (only those with RR mode)
        reusableRoundRobinRules.clear();
        for (SortRule rule : reusableMatchingRules) {
            SortRule.DistributionMode effectiveMode = getEffectiveMode(rule);
            if (effectiveMode == SortRule.DistributionMode.ROUND_ROBIN) {
                reusableRoundRobinRules.add(rule);
            }
        }

        if (!reusableRoundRobinRules.isEmpty()) {
            roundRobinIndex = roundRobinIndex % reusableRoundRobinRules.size();
            SortRule selectedRule = reusableRoundRobinRules.get(roundRobinIndex);
            roundRobinIndex++;
            return selectedRule.outputFace;
        }

        return defaultSide;
    }

    /**
     * Get effective distribution mode for a rule, considering global setting and
     * rule override.
     */
    private SortRule.DistributionMode getEffectiveMode(SortRule rule) {
        switch (rule.distributionOverride) {
            case USE_GLOBAL:
                return distributionMode;
            case FORCE_FIRST_MATCH:
                return SortRule.DistributionMode.FIRST_MATCH;
            case FORCE_ROUND_ROBIN:
                return SortRule.DistributionMode.ROUND_ROBIN;
            default:
                return distributionMode;
        }
    }

    /**
     * Push item to neighbor using cached IItemHandler,
     * then BuildCraft IInjectable as fallback for BC pipes.
     *
     * @return remaining items that couldn't be inserted
     */
    private ItemStack pushItemToSide(ItemStack stack, EnumFacing side) {
        // OPTYMALIZACJA: Użyj cache zamiast sprawdzać capabilities co tick
        IItemHandler handler = neighborHandlerCache.get(side);
        if (handler != null) {
            ItemStack result = ItemHandlerHelper.insertItem(handler, stack, false);
            if (result.getCount() < stack.getCount()) {
                return result; // Successfully inserted some items
            }
        }

        // Fallback: BuildCraft IInjectable (z cache)
        IInjectable injectable = neighborInjectableCache.get(side);
        if (injectable != null) {
            try {
                EnumFacing into = side.getOpposite();
                if (injectable.canInjectItems(into)) {
                    // injectItem returns leftover items, null color, default speed
                    ItemStack remaining = injectable.injectItem(stack, true, into, null, 0.02);
                    if (remaining == null) {
                        return stack; // Safety check for bad API implementations
                    }
                    return remaining;
                }
            } catch (Throwable t) {
                // Catch ALL errors from external mods
                // Remove from cache to avoid repeated errors/spam if it's broken
                neighborInjectableCache.remove(side);
                return stack;
            }
        }

        // No compatible handler found - keep item in buffer
        return stack;
    }

    /**
     * Check if there's a valid output on the given side.
     */
    public boolean hasInventoryOnSide(EnumFacing side) {
        if (world == null)
            return false;
        TileEntity neighbor = world.getTileEntity(pos.offset(side));
        if (neighbor == null)
            return false;

        // Any TileEntity counts as potential output (for BC pipes etc.)
        return true;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        inventory.deserializeNBT(compound.getCompoundTag("Inventory"));
        if (compound.hasKey("Rules")) {
            rules = RuleSerializer.deserializeList(compound.getTagList("Rules", Constants.NBT.TAG_COMPOUND));
        }
        if (compound.hasKey("DefaultSide")) {
            defaultSide = EnumFacing.getFront(compound.getInteger("DefaultSide"));
        }
        if (compound.hasKey("DistributionMode")) {
            int mode = compound.getInteger("DistributionMode");
            distributionMode = SortRule.DistributionMode.values()[mode % SortRule.DistributionMode.values().length];
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Inventory", inventory.serializeNBT());
        compound.setTag("Rules", RuleSerializer.serializeList(rules));
        compound.setInteger("DefaultSide", defaultSide.getIndex());
        compound.setInteger("DistributionMode", distributionMode.ordinal());
        return compound;
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 1, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        readFromNBT(tag);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    /**
     * Insert-only wrapper that prevents external pipes from extracting items.
     * Mekanism and other mods can insert items but not extract them.
     */
    private final IItemHandler insertOnlyHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return inventory.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            // Show current contents for slot validation
            return inventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            // Require at least one rule (preset) to be defined before accepting items
            if (rules.isEmpty()) {
                return stack;
            }
            return inventory.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            // Block extraction from outside
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        // Note: isItemValid is a default method in IItemHandler that returns true
        // We don't need to override it - default behavior allows all items
    };

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(insertOnlyHandler);
        }
        return super.getCapability(capability, facing);
    }

    /**
     * Get the real inventory for internal use (GUI, slots)
     */
    public IItemHandler getInventory() {
        return inventory;
    }
}
