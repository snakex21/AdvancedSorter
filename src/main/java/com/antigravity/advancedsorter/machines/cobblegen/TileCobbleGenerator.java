package com.antigravity.advancedsorter.machines.cobblegen;

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

public class TileCobbleGenerator extends TileEntity implements ITickable {

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

    // Energy
    private final EnergyStorage energyStorage = new EnergyStorage(50000, 1000, 0);
    private static final int ENERGY_PER_OPERATION = 100; // Configurable?
    private static final int TICKS_PER_OPERATION = 20; // 1 second

    // Fluids (1 bucket of each is enough for infinite gen)
    private final FluidTank waterTank = new FluidTank(1000) {
        @Override
        public boolean canDrain() { return false; } // Don't allow extracting
        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            return fluid.getFluid() == FluidRegistry.WATER;
        }
    };

    private final FluidTank lavaTank = new FluidTank(1000) {
        @Override
        public boolean canDrain() { return false; } // Don't allow extracting
        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            return fluid.getFluid() == FluidRegistry.LAVA;
        }
    };

    // Item Output
    private final ItemStackHandler outputInventory = new ItemStackHandler(1);

    private int progress = 0;

    // OPTYMALIZACJA: Sleep timer - nie sprawdzaj co tick jeśli wyjście pełne
    private int sleepTimer = 0;

    @Override
    public void update() {
        if (world.isRemote) return;

        if (sleepTimer > 0) {
            sleepTimer--;
            return;
        }

        // Check if we have infinite sources
        boolean hasWater = waterTank.getFluidAmount() >= 1000;
        boolean hasLava = lavaTank.getFluidAmount() >= 1000;

        if (hasWater && hasLava) {
            // Check energy
            if (energyStorage.getEnergyStored() >= ENERGY_PER_OPERATION) {
                // Check output space
                ItemStack currentOutput = outputInventory.getStackInSlot(0);
                if (currentOutput.isEmpty() || (currentOutput.getItem() == Item.getItemFromBlock(Blocks.COBBLESTONE) && currentOutput.getCount() < 64)) {

                    progress++;
                    if (progress >= TICKS_PER_OPERATION) {
                        progress = 0;

                        // Consume energy
                        energyStorage.extractEnergy(ENERGY_PER_OPERATION, false);

                        // Produce Cobble
                        outputInventory.insertItem(0, new ItemStack(Blocks.COBBLESTONE), false);

                        markDirty();
                    }
                } else {
                    // Output full
                    sleepTimer = 40; // Sleep 2 seconds
                }
            } else {
                // Not enough energy
                sleepTimer = 20;
            }
        } else {
            // Missing fluids
            sleepTimer = 40;
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

    public int getLavaAmount() {
        return lavaTank.getFluidAmount();
    }

    public int getProgress() {
        return progress;
    }

    // Setters for Container syncing
    public void setEnergyStored(int energy) {
        if (world.isRemote && ENERGY_FIELD != null) { // Only on client
             try {
                 ENERGY_FIELD.setInt(energyStorage, energy);
            } catch (Exception e) {}
        }
    }

    public void setWaterAmount(int amount) {
        if (world.isRemote) {
            FluidStack fluid = new FluidStack(FluidRegistry.WATER, amount);
            waterTank.setFluid(fluid);
        }
    }

    public void setLavaAmount(int amount) {
        if (world.isRemote) {
            FluidStack fluid = new FluidStack(FluidRegistry.LAVA, amount);
            lavaTank.setFluid(fluid);
        }
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    // Capability Wrapper for combined fluid tanks (so pipes can fill both)
    private final net.minecraftforge.fluids.capability.IFluidHandler combinedFluidHandler = new net.minecraftforge.fluids.capability.IFluidHandler() {
        @Override
        public net.minecraftforge.fluids.capability.IFluidTankProperties[] getTankProperties() {
            return new net.minecraftforge.fluids.capability.IFluidTankProperties[] {
                waterTank.getTankProperties()[0],
                lavaTank.getTankProperties()[0]
            };
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            if (resource == null) return 0;
            if (resource.getFluid() == FluidRegistry.WATER) {
                return waterTank.fill(resource, doFill);
            } else if (resource.getFluid() == FluidRegistry.LAVA) {
                return lavaTank.fill(resource, doFill);
            }
            return 0;
        }

        @Nullable
        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) { return null; }

        @Nullable
        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) { return null; }
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
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(outputInventory);
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(combinedFluidHandler);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Output", outputInventory.serializeNBT());
        compound.setTag("WaterTank", waterTank.writeToNBT(new NBTTagCompound()));
        compound.setTag("LavaTank", lavaTank.writeToNBT(new NBTTagCompound()));
        compound.setInteger("Energy", energyStorage.getEnergyStored());
        compound.setInteger("Progress", progress);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("Output")) outputInventory.deserializeNBT(compound.getCompoundTag("Output"));
        if (compound.hasKey("WaterTank")) waterTank.readFromNBT(compound.getCompoundTag("WaterTank"));
        if (compound.hasKey("LavaTank")) lavaTank.readFromNBT(compound.getCompoundTag("LavaTank"));

        // EnergyStorage doesn't have built-in NBT methods in 1.12 sometimes, setting manually via reflection or recreating
        // but here we can just use receive/extract hacks or reflection.
        // Simpler way:
        int energy = compound.getInteger("Energy");
        // Reset and fill
        if (ENERGY_FIELD != null) {
            try {
                ENERGY_FIELD.setInt(energyStorage, energy);
            } catch (Exception e) {}
        }

        progress = compound.getInteger("Progress");
    }
}
