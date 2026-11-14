package com.antigravity.advancedsorter.machines.crucible;

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

public class TileCrucible extends TileEntity implements ITickable {

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

    // High energy storage for speed
    private final EnergyStorage energyStorage = new EnergyStorage(100000, 5000, 0);

    // Config: 1 Cobble + 500 RF -> 1000 mB Lava (1 Cobble = 1 Bucket)
    // Speed: 1 Operation every 5 ticks (0.25s) -> 1 Bucket every 0.25 seconds at full power
    private static final int ENERGY_PER_OPERATION = 500;
    private static final int LAVA_PER_OPERATION = 1000;
    private static final int TICKS_PER_OPERATION = 5;

    private final FluidTank lavaTank = new FluidTank(16000); // 16 Buckets - output only tank

    // Wrapper that only allows draining (for external access via pipes)
    private final FluidTank outputOnlyTank = new FluidTank(16000) {
        @Override
        public FluidStack getFluid() {
            return lavaTank.getFluid();
        }

        @Override
        public int getFluidAmount() {
            return lavaTank.getFluidAmount();
        }

        @Override
        public int getCapacity() {
            return lavaTank.getCapacity();
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return 0; // Block external filling
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            return lavaTank.drain(resource, doDrain);
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            return lavaTank.drain(maxDrain, doDrain);
        }
    };

    private final ItemStackHandler inputInventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // Accept Cobblestone variants? For now just vanilla Cobblestone
            return stack.getItem() == Item.getItemFromBlock(Blocks.COBBLESTONE);
        }
    };

    private int progress = 0;

    // OPTYMALIZACJA: Sleep timer
    private int sleepTimer = 0;

    @Override
    public void update() {
        if (world.isRemote) return;

        if (sleepTimer > 0) {
            sleepTimer--;
            return;
        }

        ItemStack input = inputInventory.getStackInSlot(0);

        if (!input.isEmpty() && input.getItem() == Item.getItemFromBlock(Blocks.COBBLESTONE)) {
            // Check limits
            if (lavaTank.getFluidAmount() + LAVA_PER_OPERATION <= lavaTank.getCapacity()) {
                if (energyStorage.getEnergyStored() >= ENERGY_PER_OPERATION) {

                    progress++;
                    if (progress >= TICKS_PER_OPERATION) {
                        progress = 0;

                        // Consume
                        inputInventory.extractItem(0, 1, false);
                        energyStorage.extractEnergy(ENERGY_PER_OPERATION, false);

                        // Produce
                        lavaTank.fill(new FluidStack(FluidRegistry.LAVA, LAVA_PER_OPERATION), true);

                        markDirty();
                    }
                } else {
                    // Not enough energy
                    sleepTimer = 20;
                }
            } else {
                // Output tank full
                sleepTimer = 40;
            }
        } else {
            progress = 0;
            // No valid input
            sleepTimer = 20;
        }
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return energyStorage.getMaxEnergyStored();
    }

    public int getLavaAmount() {
        return lavaTank.getFluidAmount();
    }

    public int getLavaCapacity() {
        return lavaTank.getCapacity();
    }

    public int getProgress() {
        return progress;
    }

    // Setters for Container syncing
    public void setEnergyStored(int energy) {
        if (world.isRemote && ENERGY_FIELD != null) {
             try {
                 ENERGY_FIELD.setInt(energyStorage, energy);
            } catch (Exception e) {}
        }
    }

    public void setLavaAmount(int amount) {
        if (world.isRemote) {
            if (amount > 0) {
                FluidStack fluid = new FluidStack(FluidRegistry.LAVA, amount);
                lavaTank.setFluid(fluid);
            } else {
                lavaTank.setFluid(null);
            }
        }
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    // Helper for manual interaction
    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        return inputInventory.insertItem(0, stack, simulate);
    }

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
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inputInventory);
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            // Use output-only wrapper for external access (pipes)
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(outputOnlyTank);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Input", inputInventory.serializeNBT());
        compound.setTag("LavaTank", lavaTank.writeToNBT(new NBTTagCompound()));
        compound.setInteger("Energy", energyStorage.getEnergyStored());
        compound.setInteger("Progress", progress);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("Input")) inputInventory.deserializeNBT(compound.getCompoundTag("Input"));
        if (compound.hasKey("LavaTank")) lavaTank.readFromNBT(compound.getCompoundTag("LavaTank"));

        int energy = compound.getInteger("Energy");
        if (ENERGY_FIELD != null) {
            try {
                ENERGY_FIELD.setInt(energyStorage, energy);
            } catch (Exception e) {}
        }

        progress = compound.getInteger("Progress");
    }
}
