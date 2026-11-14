package com.antigravity.advancedsorter.machines.voidminer;

import com.antigravity.advancedsorter.tiles.TileBasicEnergy;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.WeightedRandom;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TileVoidMiner extends TileBasicEnergy implements ITickable {

    private final ItemStackHandler inventory = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }
    };

    // OPTYMALIZACJA: Cache dla listy rud
    private boolean oreCacheDirty = true;

    // Filter inventory (ghost slots logic handled in GUI/Container usually, but here we just need storage)
    private final ItemStackHandler filterInventory = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            oreCacheDirty = true;
            markDirty();
        }
    };

    private int timer = 0;
    private static final int OPERATION_COST = 5000; // Expensive!
    private static final int SPEED = 100; // 5 seconds

    // OPTYMALIZACJA: Reużywalna lista dla dozwolonych rud
    private final List<WeightedOre> reusableAllowedOres = new ArrayList<>();

    private static final List<WeightedOre> ORE_TABLE = new ArrayList<>();

    static {
        ORE_TABLE.add(new WeightedOre(new ItemStack(Blocks.COBBLESTONE), 100));
        ORE_TABLE.add(new WeightedOre(new ItemStack(Blocks.STONE, 1, 1), 80)); // Granite
        ORE_TABLE.add(new WeightedOre(new ItemStack(Blocks.STONE, 1, 3), 80)); // Diorite
        ORE_TABLE.add(new WeightedOre(new ItemStack(Blocks.STONE, 1, 5), 80)); // Andesite
        ORE_TABLE.add(new WeightedOre(new ItemStack(Blocks.COAL_ORE), 60));
        ORE_TABLE.add(new WeightedOre(new ItemStack(Blocks.IRON_ORE), 40));
        ORE_TABLE.add(new WeightedOre(new ItemStack(Blocks.REDSTONE_ORE), 30));
        ORE_TABLE.add(new WeightedOre(new ItemStack(Blocks.GOLD_ORE), 20));
        ORE_TABLE.add(new WeightedOre(new ItemStack(Blocks.LAPIS_ORE), 20));
        ORE_TABLE.add(new WeightedOre(new ItemStack(Blocks.DIAMOND_ORE), 5));
        ORE_TABLE.add(new WeightedOre(new ItemStack(Blocks.EMERALD_ORE), 2));
    }

    public TileVoidMiner() {
        super(1000000, 10000); // 1M capacity, 10k input
    }

    @Override
    public void update() {
        if (world.isRemote) return;

        if (storage.getEnergyStored() >= OPERATION_COST) {
            timer++;
            if (timer >= SPEED) {
                timer = 0;
                performMine();
            }
        }
    }

    private void performMine() {
        // OPTYMALIZACJA: Sprawdź czy jest miejsce w ekwipunku PRZED wykonaniem pracy/zużyciem energii
        boolean hasSpace = false;
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (inventory.getStackInSlot(i).getCount() < inventory.getSlotLimit(i)) {
                hasSpace = true;
                break;
            }
        }

        if (!hasSpace) {
            return; // Don't mine if full
        }

        storage.extractEnergy(OPERATION_COST, false);

        // OPTYMALIZACJA: Przebuduj cache tylko gdy filtr się zmienił
        if (oreCacheDirty) {
            rebuildOreCache();
        }

        // If no filter set, use all ores. If filter set but no matches found in table, use empty list (nothing mined)
        // Check if filter is empty (meaning no items in filter slots)
        boolean hasFilterItems = false;
        for(int i=0; i<filterInventory.getSlots(); i++) {
            if(!filterInventory.getStackInSlot(i).isEmpty()) {
                hasFilterItems = true;
                break;
            }
        }

        List<WeightedOre> pool = hasFilterItems ? reusableAllowedOres : ORE_TABLE;

        if (!pool.isEmpty()) {
            ItemStack mined = WeightedRandom.getRandomItem(world.rand, pool).stack.copy();
            insertItem(mined);
        }
    }

    private void rebuildOreCache() {
        reusableAllowedOres.clear();
        for (int i = 0; i < filterInventory.getSlots(); i++) {
            ItemStack filterStack = filterInventory.getStackInSlot(i);
            if (!filterStack.isEmpty()) {
                // Find matching ores in the ORE_TABLE
                for (WeightedOre ore : ORE_TABLE) {
                    if (ore.stack.isItemEqual(filterStack)) {
                        reusableAllowedOres.add(ore);
                    }
                }
            }
        }
        oreCacheDirty = false;
    }

    public ItemStackHandler getFilterInventory() {
        return filterInventory;
    }

    private void insertItem(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < inventory.getSlots(); i++) {
            remaining = inventory.insertItem(i, remaining, false);
            if (remaining.isEmpty()) break;
        }
        // Spawn remaining if full
        if (!remaining.isEmpty()) {
            InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), remaining);
        }
    }

    public void dropInventory() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
        }
        return super.getCapability(capability, facing);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        inventory.deserializeNBT(compound.getCompoundTag("Inventory"));
        if (compound.hasKey("FilterInventory")) {
            filterInventory.deserializeNBT(compound.getCompoundTag("FilterInventory"));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Inventory", inventory.serializeNBT());
        compound.setTag("FilterInventory", filterInventory.serializeNBT());
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

    private static class WeightedOre extends WeightedRandom.Item {
        public final ItemStack stack;

        public WeightedOre(ItemStack stack, int weight) {
            super(weight);
            this.stack = stack;
        }
    }
}
