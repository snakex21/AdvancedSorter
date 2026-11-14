package com.antigravity.advancedsorter.tiles;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.energy.tile.IEnergyEmitter;
import ic2.api.energy.tile.IEnergyAcceptor;

/**
 * Base class for TileEntities that handle Energy (Forge Energy & IC2 EU).
 */
@Optional.InterfaceList({
    @Optional.Interface(iface = "ic2.api.energy.tile.IEnergySink", modid = "ic2"),
    @Optional.Interface(iface = "ic2.api.energy.tile.IEnergySource", modid = "ic2")
})
public abstract class TileBasicEnergy extends TileEntity implements IEnergySink, IEnergySource {

    // OPTYMALIZACJA: Cache dla refleksji pola energyStorage - inicjalizowane raz dla wszystkich TileEntity
    protected static final Field ENERGY_FIELD;
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

    protected EnergyStorage storage;
    protected boolean addedToEnet = false;
    protected static final int RF_PER_EU = 4;

    // Default Tiers (LV = 1)
    protected int sinkTier = 1;
    protected int sourceTier = 1;

    public TileBasicEnergy(int capacity, int maxTransfer) {
        this(capacity, maxTransfer, maxTransfer);
    }

    public TileBasicEnergy(int capacity, int maxReceive, int maxExtract) {
        this.storage = new EnergyStorage(capacity, maxReceive, maxExtract);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!world.isRemote) {
            onLoaded();
        }
    }

    // Pass-through methods for subclasses
    public int getEnergyStored() {
        return storage.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return storage.getMaxEnergyStored();
    }

    /**
     * Set energy value on client side (for GUI sync).
     * Uses cached reflection field since EnergyStorage doesn't have a public setter.
     */
    public void setClientEnergy(int energy) {
        if (ENERGY_FIELD != null) {
            try {
                ENERGY_FIELD.setInt(storage, energy);
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    // ==========================================================
    // Forge Energy Capability
    // ==========================================================

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) return true;
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(storage);
        }
        return super.getCapability(capability, facing);
    }

    // ==========================================================
    // NBT Persistence
    // ==========================================================

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("Energy", storage.getEnergyStored());
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("Energy")) {
            int energy = compound.getInteger("Energy");
            // Reflection hack to set energy since standard EnergyStorage doesn't allow setting directly
            if (ENERGY_FIELD != null) {
                try {
                    ENERGY_FIELD.setInt(storage, energy);
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    // ==========================================================
    // IC2 Implementation (EU)
    // ==========================================================

    public void onLoaded() {
        if (!addedToEnet && !world.isRemote && Loader.isModLoaded("ic2")) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
            addedToEnet = true;
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        onChunkUnload();
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        if (addedToEnet && !world.isRemote && Loader.isModLoaded("ic2")) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
            addedToEnet = false;
        }
    }

    // --- IEnergySink (Accepts EU) ---

    @Override
    @Optional.Method(modid = "ic2")
    public double getDemandedEnergy() {
        int freeSpaceRF = storage.getMaxEnergyStored() - storage.getEnergyStored();
        return freeSpaceRF / (double) RF_PER_EU;
    }

    @Override
    @Optional.Method(modid = "ic2")
    public int getSinkTier() {
        return this.sinkTier;
    }

    @Override
    @Optional.Method(modid = "ic2")
    public double injectEnergy(EnumFacing directionFrom, double amount, double voltage) {
        int rfToAdd = (int) (amount * RF_PER_EU);
        int acceptedRF = storage.receiveEnergy(rfToAdd, false);
        double usedEU = acceptedRF / (double) RF_PER_EU;
        return amount - usedEU;
    }

    @Override
    @Optional.Method(modid = "ic2")
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, EnumFacing direction) {
        return true;
    }

    // --- IEnergySource (Emits EU) ---

    @Override
    @Optional.Method(modid = "ic2")
    public double getOfferedEnergy() {
        // Most machines consume, not produce. Defaults to 0.
        // EnergyConverter overrides this.
        return 0;
    }

    @Override
    @Optional.Method(modid = "ic2")
    public void drawEnergy(double amount) {
        int rfToRemove = (int) (amount * RF_PER_EU);
        storage.extractEnergy(rfToRemove, false);
    }

    @Override
    @Optional.Method(modid = "ic2")
    public int getSourceTier() {
        return this.sourceTier;
    }

    @Override
    @Optional.Method(modid = "ic2")
    public boolean emitsEnergyTo(IEnergyAcceptor receiver, EnumFacing direction) {
        return true;
    }
}
