package com.antigravity.advancedsorter.machines.repairer;

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

public class TileAutoRepairer extends TileEntity implements ITickable {

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

    private final EnergyStorage energyStorage = new EnergyStorage(100000, 2000, 0);
    private static final int RF_PER_POINT = 250; // Cost to repair 1 damage point
    private static final int POINTS_PER_OPERATION = 20; // Repair speed (was 5 per tick, now 20 per 4 ticks)
    private static final int OPERATION_INTERVAL = 4; // Run every 4 ticks

    // Slot 0: Input, Slot 1: Output
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) {
                return stack.isItemStackDamageable(); // Only damaged items
            }
            return false;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (slot == 0) sleepTimer = 0; // Wake up
            markDirty();
        }
    };

    private int progress = 0; // Visual progress only really
    private int tickTimer = 0;
    private int sleepTimer = 0;

    @Override
    public void update() {
        if (world.isRemote) return;

        if (sleepTimer > 0) {
            sleepTimer--;
            return;
        }

        // OPTYMALIZACJA: Batch logic - run every 4 ticks instead of every tick
        tickTimer++;
        if (tickTimer < OPERATION_INTERVAL) {
            return;
        }
        tickTimer = 0;

        ItemStack input = inventory.getStackInSlot(0);

        if (!input.isEmpty() && input.isItemDamaged()) {
            // Check if we have enough energy for at least 1 point
            if (energyStorage.getEnergyStored() >= RF_PER_POINT) {

                int damage = input.getItemDamage();
                int repairAmount = Math.min(POINTS_PER_OPERATION, damage);

                // Calculate cost
                int cost = repairAmount * RF_PER_POINT;

                // Check energy again for full amount
                int availableEnergy = energyStorage.getEnergyStored();
                if (availableEnergy < cost) {
                    // Reduce repair amount to match energy
                    repairAmount = availableEnergy / RF_PER_POINT;
                    cost = repairAmount * RF_PER_POINT;
                }

                if (repairAmount > 0) {
                    energyStorage.extractEnergy(cost, false);
                    input.setItemDamage(damage - repairAmount);

                    // If fully repaired, move to output if possible
                    if (input.getItemDamage() == 0) {
                        if (inventory.insertItem(1, input, true).isEmpty()) {
                            inventory.insertItem(1, input, false);
                            inventory.setStackInSlot(0, ItemStack.EMPTY);
                        }
                    }

                    progress++; // Just for animation
                    if (progress > 20) progress = 0;
                    markDirty();
                } else {
                    sleepTimer = 20; // Brakuje energii na naprawę choćby 1 punktu
                }
            } else {
                sleepTimer = 20; // Brakuje energii
            }
        } else {
            progress = 0;
            // Move non-damaged items from input to output immediately if they got there somehow
            if (!input.isEmpty() && !input.isItemDamaged()) {
                 if (inventory.insertItem(1, input, true).isEmpty()) {
                    inventory.insertItem(1, input, false);
                    inventory.setStackInSlot(0, ItemStack.EMPTY);
                    markDirty();
                } else {
                    sleepTimer = 20; // Output full
                }
            } else {
                sleepTimer = 20; // Empty input
            }
        }
    }

    public int getEnergyStored() { return energyStorage.getEnergyStored(); }
    public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
    public int getProgress() { return progress; }

    public void setEnergyStored(int e) {
        if (ENERGY_FIELD != null) {
            try {
                ENERGY_FIELD.setInt(energyStorage, e);
            } catch (Exception ex) {}
        }
    }
    public void setProgress(int p) { this.progress = p; }

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
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Inventory", inventory.serializeNBT());
        compound.setInteger("Energy", energyStorage.getEnergyStored());
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("Inventory")) inventory.deserializeNBT(compound.getCompoundTag("Inventory"));
        setEnergyStored(compound.getInteger("Energy"));
    }
}
