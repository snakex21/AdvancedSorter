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

    // OPTYMALIZACJA: Sleep timer gdy nie można znaleźć miejsca
    private int sleepTimer = 0;
    private static final int SLEEP_DURATION = 20; // Śpij 1 sekundę gdy brak miejsca

    @Override
    public void update() {
        if (world.isRemote)
            return;

        // OPTYMALIZACJA: Jeśli śpimy, tylko zmniejsz licznik
        if (sleepTimer > 0) {
            sleepTimer--;
            return;
        }

        // Try to place multiple blocks per tick if we have enough fluid
        // This makes it feel like it's flowing at the speed of the pipe (e.g. diamond
        // pipe)
        int blocksToPlace = Math.min(tank.getFluidAmount() / 1000, 10); // Up to 10 blocks per tick
        if (blocksToPlace > 0) {
            boolean placed = tryPlaceMultipleFluids(blocksToPlace);
            // Jeśli nie udało się umieścić żadnego bloku - śpij
            if (!placed) {
                sleepTimer = SLEEP_DURATION;
            }
        }
    }

    private boolean tryPlaceMultipleFluids(int count) {
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
                        boolean anyPlaced = false;
                        for (BlockPos targetPos : targets) {
                            if (tank.getFluidAmount() >= 1000) {
                                world.setBlockState(targetPos, fluidBlock.getDefaultState(), 3);
                                tank.drain(1000, true);
                                anyPlaced = true;
                            }
                        }
                        return anyPlaced;
                    }
                }
            }
        }
        return false; // Nie udało się nic umieścić
    }

    // OPTYMALIZACJA: Reużywalne kolekcje dla BFS
    private final Queue<BlockPos> bfsQueue = new LinkedList<>();
    private final Set<BlockPos> bfsVisited = new HashSet<>();
    private final List<BlockPos> bfsLowestPositions = new ArrayList<>();

    private List<BlockPos> findLowestEmptyBlocks(EnumFacing facing, int maxCount) {
        BlockPos startPos = pos.offset(facing);
        if (world.isOutsideBuildHeight(startPos))
            return Collections.emptyList();

        // Clear reused collections
        bfsQueue.clear();
        bfsVisited.clear();
        bfsLowestPositions.clear();

        bfsQueue.add(startPos);
        bfsVisited.add(startPos);

        int lowestY = pos.getY() + 1;
        int checkedCount = 0;
        int maxChecks = 1000; // Zmniejszono z 5000 dla wydajności

        FluidStack tankFluid = tank.getFluid();
        if (tankFluid == null)
            return Collections.emptyList();
        Block targetFluidBlock = tankFluid.getFluid().getBlock();

        while (!bfsQueue.isEmpty() && checkedCount < maxChecks) {
            BlockPos current = bfsQueue.poll();
            checkedCount++;

            if (isValidFluidPlacement(current)) {
                if (current.getY() < lowestY) {
                    lowestY = current.getY();
                    bfsLowestPositions.clear();
                    bfsLowestPositions.add(current);
                } else if (current.getY() == lowestY) {
                    bfsLowestPositions.add(current);
                }
            }

            if (bfsLowestPositions.size() >= maxCount * 2 && lowestY < pos.getY()) {
                break;
            }

            for (EnumFacing dir : EnumFacing.VALUES) {
                BlockPos neighbor = current.offset(dir);
                // Sprawdź czy już odwiedzono zanim sprawdzisz isOutsideBuildHeight (szybsze)
                if (bfsVisited.contains(neighbor)) continue;

                if (!world.isOutsideBuildHeight(neighbor)) {
                    if (neighbor.distanceSq(pos) > 64 * 64)
                        continue;

                    // Add to visited immediately
                    bfsVisited.add(neighbor);

                    IBlockState nState = world.getBlockState(neighbor);
                    boolean passable = world.isAirBlock(neighbor) || nState.getBlock().isReplaceable(world, neighbor);
                    if (!passable && targetFluidBlock != null && nState.getBlock() == targetFluidBlock) {
                        passable = true;
                    }

                    if (passable) {
                        bfsQueue.add(neighbor);
                    }
                }
            }
        }

        if (bfsLowestPositions.isEmpty())
            return Collections.emptyList();

        // Zamiast Collections.shuffle co tworzy nową listę/tablicę, weź pierwsze N
        // Opcjonalnie można losować w prostszy sposób jeśli to potrzebne dla estetyki
        // Ale dla wydajności bierzemy po kolei.
        List<BlockPos> result = new ArrayList<>(Math.min(bfsLowestPositions.size(), maxCount));
        for (int i = 0; i < Math.min(bfsLowestPositions.size(), maxCount); i++) {
             result.add(bfsLowestPositions.get(i));
        }
        return result;
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
