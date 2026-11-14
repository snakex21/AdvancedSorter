package com.antigravity.advancedsorter.machines.washer;

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
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

public class TileWasher extends TileEntity implements ITickable {

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
    private static final int ENERGY_PER_OPERATION = 200;
    private static final int WATER_PER_OPERATION = 250; // 250 mB per Clay Block
    private static final int TICKS_PER_OPERATION = 20;

    private final FluidTank waterTank = new FluidTank(4000) {
        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            return fluid.getFluid() == FluidRegistry.WATER;
        }
    };

    // Slot 0: Input (Sand/Concrete Powder), Slot 1: Output (Clay/Concrete)
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) {
                return stack.getItem() == Item.getItemFromBlock(Blocks.SAND) ||
                       stack.getItem() == Item.getItemFromBlock(Blocks.CONCRETE_POWDER);
            }
            return false;
        }
    };

    private int progress = 0;

    // OPTYMALIZACJA: Sleep timer - nie sprawdzaj receptur co tick gdy nie ma inputu
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

            // Recipe 1: Sand -> Clay
            if (input.getItem() == Item.getItemFromBlock(Blocks.SAND)) {
                result = new ItemStack(Blocks.CLAY);
                validRecipe = true;
            }
            // Recipe 2: Concrete Powder -> Concrete
            else if (input.getItem() == Item.getItemFromBlock(Blocks.CONCRETE_POWDER)) {
                // Metadata defines color, same for powder and block
                result = new ItemStack(Blocks.CONCRETE, 1, input.getMetadata());
                validRecipe = true;
            }

            if (validRecipe) {
                if (waterTank.getFluidAmount() >= WATER_PER_OPERATION) {
                    if (inventory.insertItem(1, result, true).isEmpty()) {

                        if (energyStorage.getEnergyStored() >= ENERGY_PER_OPERATION) {
                            progress++;
                            if (progress >= TICKS_PER_OPERATION) {
                                progress = 0;

                                // Consume
                                inventory.extractItem(0, 1, false);
                                waterTank.drain(WATER_PER_OPERATION, true);
                                energyStorage.extractEnergy(ENERGY_PER_OPERATION, false);

                                // Produce
                                inventory.insertItem(1, result, false);

                                markDirty();
                            }
                        }
                    } else {
                        // Output full
                        sleepTimer = 20;
                    }
                } else {
                    // Not enough water
                    sleepTimer = 20;
                }
            } else {
                // Invalid item in input
                progress = 0;
                sleepTimer = 20;
            }
        } else {
            // Empty input
            progress = 0;
            sleepTimer = 20;
        }
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return energyStorage.getMaxEnergyStored();
    }

    public int getWaterAmount() {
        return waterTank.getFluidAmount();
    }

    public int getWaterCapacity() {
        return waterTank.getCapacity();
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

    public void setWaterAmount(int amount) {
        if (world.isRemote) {
            if (amount > 0) {
                FluidStack fluid = new FluidStack(FluidRegistry.WATER, amount);
                waterTank.setFluid(fluid);
            } else {
                waterTank.setFluid(null);
            }
        }
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        return inventory.insertItem(0, stack, simulate);
    }

    public ItemStackHandler getInternalInventory() {
        return inventory;
    }

    private final net.minecraftforge.items.IItemHandler automatedHandler = new net.minecraftforge.items.IItemHandler() {
        @Override
        public int getSlots() { return 2; }
        @Override
        public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot == 0) return inventory.insertItem(slot, stack, simulate);
            return stack;
        }
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
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
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return true;
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
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(waterTank);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Inventory", inventory.serializeNBT());
        compound.setTag("WaterTank", waterTank.writeToNBT(new NBTTagCompound()));
        compound.setInteger("Energy", energyStorage.getEnergyStored());
        compound.setInteger("Progress", progress);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("Inventory")) inventory.deserializeNBT(compound.getCompoundTag("Inventory"));
        if (compound.hasKey("WaterTank")) waterTank.readFromNBT(compound.getCompoundTag("WaterTank"));

        int energy = compound.getInteger("Energy");
        if (energyField != null) {
            try {
                energyField.setInt(energyStorage, energy);
            } catch (Exception e) {}
        }

        progress = compound.getInteger("Progress");
    }
}
