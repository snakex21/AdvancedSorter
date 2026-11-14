package com.antigravity.advancedsorter.machines.flintmaker;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

public class TileFlintMaker extends TileEntity implements ITickable {

    // OPTYMALIZACJA: Cache dla pola energii
    private static final Field ENERGY_FIELD;
    static {
        Field f = null;
        try {
            f = EnergyStorage.class.getDeclaredField("energy");
            f.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        ENERGY_FIELD = f;
    }

    private final EnergyStorage energyStorage = new EnergyStorage(50000, 1000, 0);
    private static final int ENERGY_PER_OPERATION = 400;
    private static final int TICKS_PER_OPERATION = 40; // 2 seconds

    // Slot 0: Input, Slot 1: Output
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) {
                return stack.getItem() == Item.getItemFromBlock(Blocks.GRAVEL);
            }
            return false;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (slot == 0) sleepTimer = 0; // Wake up logic
            markDirty();
        }
    };

    private int progress = 0;
    private int sleepTimer = 0;

    @Override
    public void update() {
        if (world.isRemote) return;

        if (sleepTimer > 0) {
            sleepTimer--;
            return;
        }

        ItemStack input = inventory.getStackInSlot(0);

        if (!input.isEmpty() && input.getItem() == Item.getItemFromBlock(Blocks.GRAVEL)) {
            // Check output space
            ItemStack result = new ItemStack(Items.FLINT);
            if (inventory.insertItem(1, result, true).isEmpty()) { // Check if fits

                if (energyStorage.getEnergyStored() >= ENERGY_PER_OPERATION) {
                    progress++;
                    if (progress >= TICKS_PER_OPERATION) {
                        progress = 0;

                        // Consume resources
                        inventory.extractItem(0, 1, false);
                        energyStorage.extractEnergy(ENERGY_PER_OPERATION, false);

                        // Produce output
                        inventory.insertItem(1, result, false);

                        markDirty();
                    }
                } else {
                    sleepTimer = 20; // Brakuje energii
                }
            } else {
                sleepTimer = 20; // Output full
            }
        } else {
            progress = 0;
            sleepTimer = 20; // Empty or invalid item
        }
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return energyStorage.getMaxEnergyStored();
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return TICKS_PER_OPERATION;
    }

    public void setEnergyStored(int energy) {
        if (world.isRemote && ENERGY_FIELD != null) {
             try {
                 ENERGY_FIELD.setInt(energyStorage, energy);
            } catch (Exception e) {}
        }
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    // Expose internal inventory for Container/GUI
    public ItemStackHandler getInventory() {
        return inventory;
    }

    // Automated Item Handling wrapper
    private final net.minecraftforge.items.IItemHandler automatedHandler = new net.minecraftforge.items.IItemHandler() {
        @Override
        public int getSlots() { return 2; }
        @Override
        public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            // Only allow insert to slot 0
            if (slot == 0) return inventory.insertItem(slot, stack, simulate);
            return stack;
        }
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            // Only allow extract from slot 1
            if (slot == 1) return inventory.extractItem(slot, amount, simulate);
            return ItemStack.EMPTY;
        }
        @Override
        public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
    };

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) return true;
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(energyStorage);
        }
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(automatedHandler);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Inventory", inventory.serializeNBT());
        compound.setInteger("Energy", energyStorage.getEnergyStored());
        compound.setInteger("Progress", progress);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("Inventory")) inventory.deserializeNBT(compound.getCompoundTag("Inventory"));

        int energy = compound.getInteger("Energy");
        if (ENERGY_FIELD != null) {
            try {
                ENERGY_FIELD.setInt(energyStorage, energy);
            } catch (Exception e) {}
        }

        progress = compound.getInteger("Progress");
    }
}
