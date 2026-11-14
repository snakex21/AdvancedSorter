package com.antigravity.advancedsorter.machines.crusher;

import net.minecraft.init.Blocks;
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
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

public class TileCrusher extends TileEntity implements ITickable {

    // OPTYMALIZACJA: Cache dla refleksji pola energyStorage
    private static Field energyField;
    static {
        try {
            energyField = EnergyStorage.class.getDeclaredField("energy");
            energyField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private final EnergyStorage energyStorage = new EnergyStorage(50000, 1000, 0);
    private static final int ENERGY_PER_OPERATION = 400;
    private static final int TICKS_PER_OPERATION = 40; // 2 seconds

    // Slot 0: Input, Slot 1: Output
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) {
                return stack.getItem() == Item.getItemFromBlock(Blocks.COBBLESTONE) ||
                       stack.getItem() == Item.getItemFromBlock(Blocks.GRAVEL);
            }
            return false;
        }
    };

    private int progress = 0;
    private int crusherMode = 0; // 0 = Gravel, 1 = Sand

    // OPTYMALIZACJA: Sleep timer
    private int sleepTimer = 0;

    @Override
    public void update() {
        if (world.isRemote) return;

        if (sleepTimer > 0) {
            sleepTimer--;
            return;
        }

        ItemStack input = inventory.getStackInSlot(0);

        if (!input.isEmpty()) {
            ItemStack result = ItemStack.EMPTY;
            boolean validRecipe = false;

            // Define Recipes based on Mode
            if (input.getItem() == Item.getItemFromBlock(Blocks.COBBLESTONE)) {
                if (crusherMode == 0) {
                    result = new ItemStack(Blocks.GRAVEL);
                } else {
                    result = new ItemStack(Blocks.SAND);
                }
                validRecipe = true;
            } else if (input.getItem() == Item.getItemFromBlock(Blocks.GRAVEL)) {
                result = new ItemStack(Blocks.SAND);
                validRecipe = true;
            }

            if (validRecipe) {
                // Check output space
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
                    }
                } else {
                    // Output full
                    sleepTimer = 20;
                }
            } else {
                progress = 0;
                sleepTimer = 20; // Invalid item
            }
        } else {
            progress = 0;
            sleepTimer = 20; // Empty
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

    public void setEnergyStored(int energy) {
        if (world.isRemote && energyField != null) {
            try {
                energyField.setInt(energyStorage, energy);
            } catch (Exception e) {}
        }
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getMode() {
        return crusherMode;
    }

    public void setMode(int mode) {
        this.crusherMode = mode;
        markDirty();
    }

    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        return inventory.insertItem(0, stack, simulate);
    }

    public ItemStackHandler getInternalInventory() {
        return inventory;
    }

    // Expose Input (0) to all sides, Output (1) to all sides for simplicity (or use wrappers for sidedness)
    // For now, simple wrapper: Input to 0, Extract from 1
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
        compound.setInteger("Mode", crusherMode);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("Inventory")) inventory.deserializeNBT(compound.getCompoundTag("Inventory"));

        int energy = compound.getInteger("Energy");
        if (energyField != null) {
            try {
                energyField.setInt(energyStorage, energy);
            } catch (Exception e) {}
        }

        progress = compound.getInteger("Progress");
        crusherMode = compound.getInteger("Mode");
    }
}
