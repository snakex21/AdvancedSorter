package com.antigravity.advancedsorter.machines.placer;

import com.antigravity.advancedsorter.tiles.TileBasicEnergy;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import java.util.UUID;

import javax.annotation.Nullable;

public class TileBlockPlacer extends TileBasicEnergy implements ITickable {

    private final ItemStackHandler inventory = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
            // Check if we have any items
            hasItems = false;
            for(int i=0; i<getSlots(); i++) {
                if(!getStackInSlot(i).isEmpty()) {
                    hasItems = true;
                    break;
                }
            }
        }
    };

    private int timer = 0;
    private static final int OPERATION_COST = 200;
    private static final int SPEED = 20;

    // OPTYMALIZACJA: Flaga czy mamy przedmioty, by unikać pętli gdy pusty
    private boolean hasItems = false;

    private FakePlayer fakePlayer;

    public TileBlockPlacer() {
        super(10000, 100);
    }

    private FakePlayer getFakePlayer() {
        if (fakePlayer == null && world instanceof WorldServer) {
            fakePlayer = FakePlayerFactory.get((WorldServer) world, new GameProfile(UUID.nameUUIDFromBytes("AdvancedSorterPlacer".getBytes()), "[AdvancedSorterPlacer]"));
        }
        return fakePlayer;
    }

    @Override
    public void update() {
        if (world.isRemote) return;

        // Skip if no items to place
        if (!hasItems) return;

        if (storage.getEnergyStored() >= OPERATION_COST) {
            timer++;
            if (timer >= SPEED) {
                timer = 0;
                performPlace();
            }
        }
    }

    private void performPlace() {
        EnumFacing facing = world.getBlockState(pos).getValue(BlockBlockPlacer.FACING);
        BlockPos targetPos = pos.offset(facing);
        IBlockState targetState = world.getBlockState(targetPos);

        // Check if target is replaceable
        if (world.isAirBlock(targetPos) || targetState.getBlock().isReplaceable(world, targetPos)) {
            // Find first block in inventory
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof ItemBlock) {

                    // Use FakePlayer to place block properly (handles rotation etc.)
                    FakePlayer player = getFakePlayer();
                    if (player != null) {
                        player.setPosition(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                        // Set pitch/yaw based on facing if needed, simpler is just to rely on onItemUse

                        // Set held item
                        player.setHeldItem(EnumHand.MAIN_HAND, stack);

                        // Simulate right click on the air block?
                        // Actually ItemBlock.onItemUse needs a valid face to click against usually.
                        // But since we are placing IN FRONT, we can try to click against the block BEHIND the target?
                        // Or just use onItemUse directly on the target pos?

                        // Fallback to simpler placement if FakePlayer fails or for simple blocks?
                        // Let's stick to the previous simple logic but improved, or try onItemUse.
                        // For reliability in this mod context, let's keep it simple but fix the meta/state logic
                        // AND add the sync packets which was the main issue for "not seeing" things.

                        // Reverting to direct placement but using placement state helper if possible
                        // But wait, user said "doesn't place".

                        ItemBlock itemBlock = (ItemBlock) stack.getItem();
                        Block block = itemBlock.getBlock();
                        int meta = itemBlock.getMetadata(stack.getMetadata());
                        IBlockState stateToPlace = block.getStateForPlacement(world, targetPos, facing.getOpposite(), 0.5f, 0.5f, 0.5f, meta, player, EnumHand.MAIN_HAND);

                        if (world.setBlockState(targetPos, stateToPlace, 3)) {
                             // Play sound
                             world.playEvent(2001, targetPos, Block.getStateId(stateToPlace));

                             // TileEntity placement
                             itemBlock.placeBlockAt(stack, player, world, targetPos, facing.getOpposite(), 0.5f, 0.5f, 0.5f, stateToPlace);

                             stack.shrink(1);
                             storage.extractEnergy(OPERATION_COST, false);
                             break;
                        }
                    }
                }
            }
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
