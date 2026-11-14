package com.antigravity.advancedsorter.machines.xpvacuum;

import com.antigravity.advancedsorter.fluids.ModFluids;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.List;

public class TileXPVacuum extends TileEntity implements ITickable {

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
    private final FluidTank fluidTank = new FluidTank(16000); // 16 Buckets

    private static final int RANGE = 3;
    private static final int RF_PER_TICK = 20;
    private static final int MB_PER_XP = 20; // 1 XP Point = 20mB

    // OPTYMALIZACJA: Cache AABB i skanuj co kilka ticków
    private AxisAlignedBB cachedArea = null;
    private BlockPos cachedPos = null;
    private int scanCooldown = 0;
    private static final int MIN_SCAN_INTERVAL = 5;
    private static final int MAX_SCAN_INTERVAL = 40; // 2 seconds
    private int currentInterval = MIN_SCAN_INTERVAL;

    public TileXPVacuum() {
        fluidTank.setTileEntity(this);
        fluidTank.setCanFill(false); // Only fill from XP Orbs, not pipes
    }

    private AxisAlignedBB getSearchArea() {
        // OPTYMALIZACJA: Cache AABB jeśli pozycja się nie zmieniła
        if (cachedArea == null || !pos.equals(cachedPos)) {
            cachedArea = new AxisAlignedBB(pos).grow(RANGE, RANGE, RANGE);
            cachedPos = pos;
        }
        return cachedArea;
    }

    @Override
    public void update() {
        if (world.isRemote) return;

        // OPTYMALIZACJA: Skanuj tylko co kilka ticków
        if (scanCooldown > 0) {
            scanCooldown--;
            return;
        }

        // Reset cooldown na obecny interwał
        scanCooldown = currentInterval;

        // Drain energy for operation
        if (energyStorage.getEnergyStored() >= RF_PER_TICK) {

            // Scan for XP Orbs - używamy cache'owanego AABB
            List<EntityXPOrb> orbs = world.getEntitiesWithinAABB(EntityXPOrb.class, getSearchArea());

            boolean didWork = false;

            if (orbs.isEmpty()) {
                // Jeśli brak orbów, zwiększ interwał skanowania (zwalniamy)
                if (currentInterval < MAX_SCAN_INTERVAL) {
                    currentInterval += 5;
                }
            } else {
                // Znaleziono orby - przyspieszamy do maksimum
                currentInterval = MIN_SCAN_INTERVAL;
            }

            for (EntityXPOrb orb : orbs) {
                if (orb.isDead || orb.delayBeforeCanPickup > 0) continue;

                // Pull orb towards center
                double dx = (pos.getX() + 0.5) - orb.posX;
                double dy = (pos.getY() + 0.5) - orb.posY;
                double dz = (pos.getZ() + 0.5) - orb.posZ;
                double distSq = dx*dx + dy*dy + dz*dz;

                if (distSq <= 0.25) { // 0.5 * 0.5 = 0.25
                    // Consume Orb
                    int xpValue = orb.getXpValue();
                    int fluidAmount = xpValue * MB_PER_XP;

                    if (fluidTank.fill(new net.minecraftforge.fluids.FluidStack(ModFluids.LIQUID_XP, fluidAmount), false) == fluidAmount) {
                        fluidTank.fill(new net.minecraftforge.fluids.FluidStack(ModFluids.LIQUID_XP, fluidAmount), true);
                        orb.setDead();
                        didWork = true;
                    }
                } else {
                    // Magnetize - zwiększona siła bo rzadziej aktualizujemy
                    // Obliczamy sqrt tylko jeśli musimy przyciągnąć (rzadziej niż sprawdzenie kolizji)
                    double dist = Math.sqrt(distSq);
                    double speed = 0.15;
                    orb.motionX += (dx / dist) * speed;
                    orb.motionY += (dy / dist) * speed;
                    orb.motionZ += (dz / dist) * speed;
                    didWork = true;
                }
            }

            if (didWork) {
                energyStorage.extractEnergy(RF_PER_TICK, false);
            }
        }
    }

    // Getters for GUI
    public int getEnergyStored() { return energyStorage.getEnergyStored(); }
    public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
    public FluidTank getTank() { return fluidTank; }

    public void setEnergyStored(int energy) {
        // Reflection set
        if (ENERGY_FIELD != null) {
            try {
                ENERGY_FIELD.setInt(energyStorage, energy);
            } catch (Exception e) {}
        }
    }

    // Capabilities
    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) return CapabilityEnergy.ENERGY.cast(energyStorage);
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(fluidTank);
        return super.getCapability(capability, facing);
    }

    // NBT
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        fluidTank.writeToNBT(compound);
        compound.setInteger("Energy", energyStorage.getEnergyStored());
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        fluidTank.readFromNBT(compound);
        if (compound.hasKey("Energy")) {
            setEnergyStored(compound.getInteger("Energy"));
        }
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
}
