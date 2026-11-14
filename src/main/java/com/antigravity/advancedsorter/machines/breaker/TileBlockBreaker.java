package com.antigravity.advancedsorter.machines.breaker;

import com.antigravity.advancedsorter.tiles.TileBasicEnergy;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;

import javax.annotation.Nullable;
import java.util.List;

public class TileBlockBreaker extends TileBasicEnergy implements ITickable {

    private final ItemStackHandler inventory = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }
    };

    private int timer = 0;
    private static final int OPERATION_COST = 200; // RF per operation
    private static final int SPEED = 20; // Ticks per operation

    // OPTYMALIZACJA: Sleep timer
    private int sleepTimer = 0;

    public TileBlockBreaker() {
        super(10000, 100); // 10k capacity, 100 max receive
    }

    @Override
    public void update() {
        if (world.isRemote) return;

        if (sleepTimer > 0) {
            sleepTimer--;
            return;
        }

        if (storage.getEnergyStored() >= OPERATION_COST) {
            timer++;
            if (timer >= SPEED) {
                timer = 0;
                if (!performBreak()) {
                    sleepTimer = 20; // Sleep if nothing to break or full
                }
            }
        } else {
            sleepTimer = 20; // Sleep if no energy
        }
    }

    /**
     * @return true if operation was successful (block broken)
     */
    private boolean performBreak() {
        EnumFacing facing = world.getBlockState(pos).getValue(BlockBlockBreaker.FACING);
        BlockPos targetPos = pos.offset(facing);

        if (world.isAirBlock(targetPos)) {
            return false; // Nothing to break
        }

        IBlockState targetState = world.getBlockState(targetPos);
        Block targetBlock = targetState.getBlock();

        if (targetState.getBlockHardness(world, targetPos) >= 0) {
            // Get Drops
            List<ItemStack> drops = targetBlock.getDrops(world, targetPos, targetState, 0);

            // Try to add drops to inventory
            if (canFitDrops(drops)) {
                storage.extractEnergy(OPERATION_COST, false);
                world.destroyBlock(targetPos, false); // false = no drops (we handle them)

                for (ItemStack drop : drops) {
                    insertItem(drop);
                }
                return true;
            } else {
                return false; // Inventory full
            }
        }
        return false; // Unbreakable block
    }

    private boolean canFitDrops(List<ItemStack> drops) {
        // OPTYMALIZACJA: Symulacja na kopii ekwipunku, aby upewnić się, że WSZYSTKIE przedmioty się zmieszczą.
        // Zapobiega wyrzucaniu przedmiotów na ziemię (entity spam).
        ItemStackHandler simInventory = new ItemStackHandler(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            simInventory.setStackInSlot(i, inventory.getStackInSlot(i).copy());
        }

        for (ItemStack drop : drops) {
            ItemStack remaining = drop.copy();
            for (int i = 0; i < simInventory.getSlots(); i++) {
                remaining = simInventory.insertItem(i, remaining, false);
                if (remaining.isEmpty()) break;
            }
            // Jeśli cokolwiek zostało, znaczy że nie ma miejsca na całość
            if (!remaining.isEmpty()) return false;
        }
        return true;
    }

    private void insertItem(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < inventory.getSlots(); i++) {
            remaining = inventory.insertItem(i, remaining, false);
            if (remaining.isEmpty()) break;
        }
        // If anything remains (shouldn't happen due to check), spawn it
        if (!remaining.isEmpty()) {
            InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), remaining);
        }
    }

    public void dropInventory() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
        }
        return super.getCapability(capability, facing);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        inventory.deserializeNBT(compound.getCompoundTag("Inventory"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Inventory", inventory.serializeNBT());
        return compound;
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
