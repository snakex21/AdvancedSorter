package com.antigravity.advancedsorter.machines.stabilizer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.List;

public class TileAtmosphericStabilizer extends TileEntity implements ITickable {

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

    private final EnergyStorage energyStorage = new EnergyStorage(1000000, 50000, 0);
    private final FluidTank waterTank = new FluidTank(16000) {
        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            return fluid != null && fluid.getFluid() == FluidRegistry.WATER;
        }
    };

    // Slot 0: Bucket Input, Slot 1: Bucket Output
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            if (slot == 0) bucketSleepTimer = 0; // Wake up logic
            markDirty();
        }
    };

    private static final int ENERGY_PER_TICK = 100;
    private static final int WATER_PER_TICK = 1;
    private static final int RADIUS = 64;

    // OPTYMALIZACJA: Zmienne do kontroli częstotliwości skanowania
    private int tickCounter = 0;
    private int bucketSleepTimer = 0;
    private AxisAlignedBB cachedArea;

    @Override
    public void update() {
        if (world.isRemote) return;

        // 1. Handle Bucket Input
        if (bucketSleepTimer > 0) {
            bucketSleepTimer--;
        } else {
            ItemStack input = inventory.getStackInSlot(0);
            if (!input.isEmpty()) {
                // Try to drain water from item into tank
                // Using FluidUtil is safest for generic fluid containers
                if (FluidUtil.getFluidHandler(input) != null) {
                    // Try to fill the tank from the item
                    // This helper method handles draining the item and potentially changing it (bucket -> empty bucket)
                    // But it's tricky with ItemStackHandler directly.

                    // Manual check for water bucket for simplicity, or generic:
                    FluidStack fluidInItem = FluidUtil.getFluidContained(input);
                    if (fluidInItem != null && fluidInItem.getFluid() == FluidRegistry.WATER) {
                        // Try to fill tank
                        int filled = waterTank.fill(fluidInItem, false);
                        if (filled == fluidInItem.amount) {
                            // Can fit all
                             waterTank.fill(fluidInItem, true);

                             // Handle item change
                             if (input.getCount() == 1) {
                                 // Replace with empty container
                                 ItemStack empty = FluidUtil.getFluidHandler(input).getContainer().getItem().getContainerItem(input);
                                 if (empty.isEmpty() && input.getItem().hasContainerItem(input)) {
                                     // Fallback for vanilla bucket behavior if handler fails
                                     empty = input.getItem().getContainerItem(input);
                                 }

                                 inventory.setStackInSlot(0, empty);

                                 // Move to output if possible
                                 if (!inventory.getStackInSlot(0).isEmpty()) {
                                     ItemStack output = inventory.getStackInSlot(1);
                                     ItemStack remainder = inventory.getStackInSlot(0);

                                     if (output.isEmpty()) {
                                         inventory.setStackInSlot(1, remainder);
                                         inventory.setStackInSlot(0, ItemStack.EMPTY);
                                     } else if (output.isItemEqual(remainder) && output.getCount() + remainder.getCount() <= output.getMaxStackSize()) {
                                         output.grow(remainder.getCount());
                                         inventory.setStackInSlot(0, ItemStack.EMPTY);
                                     }
                                 }
                             } else {
                                 // Stacked buckets? Complex. Usually buckets don't stack to 64.
                                 // Just decrement one and try to push result to output.
                                 // If output full, wait.
                                 ItemStack single = input.copy();
                                 single.setCount(1);
                                 ItemStack empty = FluidUtil.getFluidHandler(single).getContainer().getItem().getContainerItem(single);

                                 if (inventory.insertItem(1, empty, true).isEmpty()) {
                                     inventory.insertItem(1, empty, false);
                                     input.shrink(1);
                                     if (input.isEmpty()) inventory.setStackInSlot(0, ItemStack.EMPTY);
                                 } else {
                                     bucketSleepTimer = 20; // Output full
                                 }
                             }
                        } else {
                            bucketSleepTimer = 20; // Tank full
                        }
                    } else {
                        bucketSleepTimer = 40; // Not water
                    }
                } else {
                    bucketSleepTimer = 40; // Not a fluid handler
                }
            } else {
                 bucketSleepTimer = 20; // Empty input
            }
        }

        // 2. Consumption logic
        // OPTYMALIZACJA: Wykonuj ciężką logikę skanowania i efektów tylko raz na sekundę (20 ticków)
        tickCounter++;
        if (tickCounter >= 20) {
            tickCounter = 0;

            boolean active = false;
            // Skalujemy koszt operacji x20, ponieważ wykonujemy ją 20 razy rzadziej
            int energyCost = ENERGY_PER_TICK * 20;
            int waterCost = WATER_PER_TICK * 20;

            if (energyStorage.getEnergyStored() >= energyCost && waterTank.getFluidAmount() >= waterCost) {

                // OPTYMALIZACJA: Cache AABB
                if (cachedArea == null) {
                    cachedArea = new AxisAlignedBB(pos).grow(RADIUS);
                }

                // Do effect
                List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, cachedArea);

                if (!players.isEmpty()) {
                    active = true;
                    for (EntityPlayer player : players) {
                        player.setAir(300);
                        // Zwiększamy czas trwania efektu do 60 ticków (3s), aby pokryć przerwę między skanowaniami (20 ticków)
                        player.addPotionEffect(new PotionEffect(MobEffects.WATER_BREATHING, 60, 0, true, false));
                        player.addPotionEffect(new PotionEffect(MobEffects.SATURATION, 60, 0, true, false));
                    }
                }

                if (active) {
                    energyStorage.extractEnergy(energyCost, false);
                    waterTank.drain(waterCost, true);
                }
            }
        }
    }

    public int getEnergyStored() { return energyStorage.getEnergyStored(); }
    public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
    public int getWaterAmount() { return waterTank.getFluidAmount(); }
    public int getWaterCapacity() { return waterTank.getCapacity(); }

    public boolean isActive() {
        return energyStorage.getEnergyStored() >= ENERGY_PER_TICK && waterTank.getFluidAmount() >= WATER_PER_TICK;
    }

    public void setEnergyStored(int e) {
        if (ENERGY_FIELD != null) {
            try {
                ENERGY_FIELD.setInt(energyStorage, e);
            } catch (Exception ex) {}
        }
    }
    public void setWaterAmount(int amount) {
        if (amount > 0) waterTank.setFluid(new FluidStack(FluidRegistry.WATER, amount));
        else waterTank.setFluid(null);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) return true;
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return true;
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) return CapabilityEnergy.ENERGY.cast(energyStorage);
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(waterTank);
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
        return super.getCapability(capability, facing);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("Energy", energyStorage.getEnergyStored());
        compound.setTag("Water", waterTank.writeToNBT(new NBTTagCompound()));
        compound.setTag("Inventory", inventory.serializeNBT());
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        setEnergyStored(compound.getInteger("Energy"));
        if (compound.hasKey("Water")) waterTank.readFromNBT(compound.getCompoundTag("Water"));
        if (compound.hasKey("Inventory")) inventory.deserializeNBT(compound.getCompoundTag("Inventory"));
    }
}
