package com.antigravity.advancedsorter.pipes.fluid;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import com.antigravity.advancedsorter.pipes.fluid.IFluidSyncable;

import javax.annotation.Nullable;
import java.util.*;

public class TileFluidOutlet extends TileEntity implements ITickable, IFluidSyncable {

    private final SyncingFluidTank tank = new SyncingFluidTank(2000, this);

    @Override
    public void update() {
        if (world.isRemote)
            return;

        // Try to place multiple blocks per tick if we have enough fluid
        // This makes it feel like it's flowing at the speed of the pipe (e.g. diamond
        // pipe)
        int blocksToPlace = Math.min(tank.getFluidAmount() / 1000, 10); // Up to 10 blocks per tick
        if (blocksToPlace > 0) {
            tryPlaceMultipleFluids(blocksToPlace);
        }
    }

    private void tryPlaceMultipleFluids(int count) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof BlockFluidOutlet) {
            EnumFacing facing = state.getValue(BlockFluidOutlet.FACING);
            List<BlockPos> targets = findLowestEmptyBlocks(facing, count);

            if (!targets.isEmpty()) {
                FluidStack fluidStack = tank.getFluid();
                if (fluidStack != null) {
                    Fluid fluid = fluidStack.getFluid();
                    Block fluidBlock = fluid.getBlock();
                    if (fluidBlock != null) {
                        for (BlockPos targetPos : targets) {
                            if (tank.getFluidAmount() >= 1000) {
                                world.setBlockState(targetPos, fluidBlock.getDefaultState(), 3);
                                tank.drain(1000, true);
                            }
                        }
                    }
                }
            }
        }
    }

    private List<BlockPos> findLowestEmptyBlocks(EnumFacing facing, int maxCount) {
        BlockPos startPos = pos.offset(facing);
        if (world.isOutsideBuildHeight(startPos))
            return Collections.emptyList();

        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> lowestPositions = new ArrayList<>();

        queue.add(startPos);
        visited.add(startPos);

        int lowestY = pos.getY() + 1;
        int checkedCount = 0;
        int maxChecks = 5000;

        FluidStack tankFluid = tank.getFluid();
        if (tankFluid == null)
            return Collections.emptyList();
        Block targetFluidBlock = tankFluid.getFluid().getBlock();

        while (!queue.isEmpty() && checkedCount < maxChecks) {
            BlockPos current = queue.poll();
            checkedCount++;

            if (isValidFluidPlacement(current)) {
                if (current.getY() < lowestY) {
                    lowestY = current.getY();
                    lowestPositions.clear();
                    lowestPositions.add(current);
                } else if (current.getY() == lowestY) {
                    lowestPositions.add(current);
                }
            }

            if (lowestPositions.size() >= maxCount * 2 && lowestY < pos.getY()) {
                break;
            }

            for (EnumFacing dir : EnumFacing.VALUES) {
                BlockPos neighbor = current.offset(dir);
                if (!visited.contains(neighbor) && !world.isOutsideBuildHeight(neighbor)) {
                    if (neighbor.distanceSq(pos) > 64 * 64)
                        continue;

                    IBlockState nState = world.getBlockState(neighbor);
                    boolean passable = world.isAirBlock(neighbor) || nState.getBlock().isReplaceable(world, neighbor);
                    if (!passable && targetFluidBlock != null && nState.getBlock() == targetFluidBlock) {
                        passable = true;
                    }

                    if (passable) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }

        if (lowestPositions.isEmpty())
            return Collections.emptyList();

        Collections.shuffle(lowestPositions);
        return lowestPositions.subList(0, Math.min(lowestPositions.size(), maxCount));
    }

    private boolean isValidFluidPlacement(BlockPos checkPos) {
        IBlockState state = world.getBlockState(checkPos);
        FluidStack tankFluid = tank.getFluid();

        boolean canPlaceHere = world.isAirBlock(checkPos) || state.getBlock().isReplaceable(world, checkPos);
        if (!canPlaceHere && tankFluid != null && state.getBlock() == tankFluid.getFluid().getBlock()) {
            if (state.getBlock() instanceof net.minecraft.block.BlockLiquid) {
                canPlaceHere = state.getValue(net.minecraft.block.BlockLiquid.LEVEL) != 0;
            } else if (state.getBlock() instanceof net.minecraftforge.fluids.IFluidBlock) {
                canPlaceHere = ((net.minecraftforge.fluids.IFluidBlock) state.getBlock()).getFilledPercentage(world,
                        checkPos) < 1.0f;
            }
        }

        if (!canPlaceHere)
            return false;
        if (checkPos.getY() <= 1)
            return true;

        BlockPos below = checkPos.down();
        IBlockState belowState = world.getBlockState(below);

        if (tankFluid != null && belowState.getBlock() == tankFluid.getFluid().getBlock()) {
            return true;
        }

        return !world.isAirBlock(below)
                && !belowState.getBlock().isReplaceable(world, below)
                && (belowState.getMaterial().isSolid() || belowState.getMaterial().isLiquid());
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        tank.readFromNBT(compound);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound = super.writeToNBT(compound);
        tank.writeToNBT(compound);
        return compound;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(tank);
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
