package com.antigravity.advancedsorter.pump;

import com.antigravity.advancedsorter.pipes.fluid.SyncingFluidTank;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import com.antigravity.advancedsorter.pipes.fluid.IFluidSyncable;

import javax.annotation.Nullable;

public class TileAdvancedPump extends TileEntity implements ITickable, IFluidSyncable {

    private final SyncingFluidTank tank = new SyncingFluidTank(16000, this);
    private final EnergyStorage energyStorage = new EnergyStorage(100000, 1000, 1000);

    private int pumpRateLimit = 1000;
    private int redstoneMode = 0;

    private int scanX, scanY, scanZ;
    private int range = 8;
    private int pumpProgress = 0;
    private int lastSecondRate = 0;
    private int currentSecondPushed = 0;
    private int tickCounter = 0;
    private int sleepTimer = 0;

    private static final int ENERGY_PER_100MB = 50; // Increased for balance

    public TileAdvancedPump() {
        resetScanner();
    }

    private void resetScanner() {
        scanX = -range;
        scanY = -range;
        scanZ = -range;
    }

    @Override
    public void update() {
        if (world.isRemote)
            return;

        if (sleepTimer > 0) {
            sleepTimer--;
            return;
        }

        if (!canOperateWithRedstone()) {
            return;
        }

        tickCounter++;
        if (tickCounter >= 20) {
            lastSecondRate = currentSecondPushed;
            currentSecondPushed = 0;
            tickCounter = 0;
        }

        int energyAvailable = energyStorage.getEnergyStored();
        int maxMbFromEnergy = (energyAvailable * 100) / ENERGY_PER_100MB;

        int mbThisTick = Math.min(pumpRateLimit / 20, maxMbFromEnergy);
        if (mbThisTick <= 0 && pumpRateLimit > 0 && maxMbFromEnergy > 0)
            mbThisTick = 1;

        pumpProgress += mbThisTick;

        while (pumpProgress >= 1000 && tank.getFluidAmount() <= tank.getCapacity() - 1000) {
            if (findAndExtractFluid()) {
                int energyCost = (1000 * ENERGY_PER_100MB) / 100;
                energyStorage.extractEnergy(energyCost, false);
                pumpProgress -= 1000;
                currentSecondPushed += 1000;
            } else {
                pumpProgress = 0;
                sleepTimer = 20;
                break;
            }
        }

        pushFluidToNeighbors();
    }

    private boolean canOperateWithRedstone() {
        if (redstoneMode == 0)
            return true;
        boolean powered = world.isBlockPowered(pos);
        if (redstoneMode == 1)
            return powered;
        if (redstoneMode == 2)
            return !powered;
        return true;
    }

    private void pushFluidToNeighbors() {
        if (tank.getFluidAmount() <= 0)
            return;

        for (EnumFacing facing : EnumFacing.VALUES) {
            TileEntity te = world.getTileEntity(pos.offset(facing));
            if (te != null && te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, facing.getOpposite())) {
                IFluidHandler handler = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY,
                        facing.getOpposite());
                if (handler != null) {
                    FluidStack drained = tank.drain(100, false);
                    if (drained != null) {
                        int filled = handler.fill(drained, true);
                        tank.drain(filled, true);
                    }
                }
            }
        }
    }

    private boolean findAndExtractFluid() {
        for (int i = 0; i < 64; i++) {
            BlockPos targetPos = pos.add(scanX, scanY, scanZ);
            IBlockState state = world.getBlockState(targetPos);

            if (isFluidSource(state, targetPos)) {
                FluidStack fluid = getFluidFromBlock(state, targetPos);
                if (fluid != null && tank.fill(fluid, false) == fluid.amount) {
                    tank.fill(fluid, true);
                    world.setBlockToAir(targetPos);
                    advanceScanner();
                    return true;
                }
            }

            advanceScanner();
        }
        return false;
    }

    private void advanceScanner() {
        scanX++;
        if (scanX > range) {
            scanX = -range;
            scanZ++;
            if (scanZ > range) {
                scanZ = -range;
                scanY++;
                if (scanY > range) {
                    resetScanner();
                }
            }
        }
    }

    private boolean isFluidSource(IBlockState state, BlockPos pos) {
        Block block = state.getBlock();
        if (block instanceof IFluidBlock) {
            return ((IFluidBlock) block).canDrain(world, pos);
        } else if (block instanceof BlockLiquid) {
            return state.getValue(BlockLiquid.LEVEL) == 0;
        }
        return false;
    }

    private FluidStack getFluidFromBlock(IBlockState state, BlockPos pos) {
        Block block = state.getBlock();
        if (block instanceof IFluidBlock) {
            return ((IFluidBlock) block).drain(world, pos, false);
        } else if (block instanceof BlockLiquid) {
            if (block == net.minecraft.init.Blocks.WATER || block == net.minecraft.init.Blocks.FLOWING_WATER) {
                return new FluidStack(FluidRegistry.WATER, 1000);
            } else if (block == net.minecraft.init.Blocks.LAVA || block == net.minecraft.init.Blocks.FLOWING_LAVA) {
                return new FluidStack(FluidRegistry.LAVA, 1000);
            }
        }
        return null;
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return energyStorage.getMaxEnergyStored();
    }

    public void setEnergyStored(int energy) {
        if (world.isRemote) {
            try {
                java.lang.reflect.Field field = EnergyStorage.class.getDeclaredField("energy");
                field.setAccessible(true);
                field.setInt(energyStorage, energy);
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }
    }

    public int getPumpRateLimit() {
        return pumpRateLimit;
    }

    public void setPumpRateLimit(int limit) {
        this.pumpRateLimit = Math.max(0, Math.min(10000, limit));
        markDirty();
    }

    public int getLastSecondRate() {
        return lastSecondRate;
    }

    public int getRedstoneMode() {
        return redstoneMode;
    }

    public void setRedstoneMode(int mode) {
        this.redstoneMode = mode % 3;
        markDirty();
    }

    public void cycleRedstoneMode() {
        setRedstoneMode((redstoneMode + 1) % 3);
    }

    public SyncingFluidTank getTank() {
        return tank;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        tank.readFromNBT(compound);

        if (compound.hasKey("Energy")) {
            setEnergyStored(compound.getInteger("Energy"));
        }

        pumpRateLimit = compound.getInteger("PumpRateLimit");
        if (pumpRateLimit == 0 && !compound.hasKey("PumpRateLimit"))
            pumpRateLimit = 1000;

        redstoneMode = compound.getInteger("RedstoneMode");
        scanX = compound.getInteger("scanX");
        scanY = compound.getInteger("scanY");
        scanZ = compound.getInteger("scanZ");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound = super.writeToNBT(compound);
        tank.writeToNBT(compound);
        compound.setInteger("Energy", energyStorage.getEnergyStored());
        compound.setInteger("PumpRateLimit", pumpRateLimit);
        compound.setInteger("RedstoneMode", redstoneMode);
        compound.setInteger("scanX", scanX);
        compound.setInteger("scanY", scanY);
        compound.setInteger("scanZ", scanZ);
        return compound;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || capability == CapabilityEnergy.ENERGY
                || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(tank);
        }
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(energyStorage);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public net.minecraft.world.World getSyncableWorld() {
        return getWorld();
    }

    @Override
    public void markSyncableDirty() {
        markDirty();
    }

    @Override
    public void requestClientSync() {
    }
}
