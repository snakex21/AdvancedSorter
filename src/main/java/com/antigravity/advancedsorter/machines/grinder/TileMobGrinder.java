package com.antigravity.advancedsorter.machines.grinder;

import com.antigravity.advancedsorter.tiles.TileBasicEnergy;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class TileMobGrinder extends TileBasicEnergy implements ITickable {

    private final ItemStackHandler inventory = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }
    };

    private int timer = 0;
    private static final int OPERATION_COST = 500;
    private static final int MIN_SPEED = 20; // 1 second
    private static final int MAX_SPEED = 100; // 5 seconds
    private int currentSpeed = MIN_SPEED;

    private FakePlayer fakePlayer;

    // OPTYMALIZACJA: Cache dla AxisAlignedBB
    private AxisAlignedBB cachedArea;
    private EnumFacing cachedFacing;

    public TileMobGrinder() {
        super(50000, 500);
    }

    private FakePlayer getFakePlayer() {
        if (fakePlayer == null && world instanceof WorldServer) {
            fakePlayer = FakePlayerFactory.get((WorldServer) world, new GameProfile(UUID.nameUUIDFromBytes("AdvancedSorterGrinder".getBytes()), "[AdvancedSorterGrinder]"));
        }
        return fakePlayer;
    }

    @Override
    public void update() {
        if (world.isRemote) return;

        if (storage.getEnergyStored() >= OPERATION_COST) {
            timer++;
            if (timer >= currentSpeed) {
                timer = 0;
                performGrind();
            }
        }
    }

    private void performGrind() {
        EnumFacing facing = world.getBlockState(pos).getValue(BlockMobGrinder.FACING);

        // OPTYMALIZACJA: Cache AxisAlignedBB - przebuduj tylko gdy facing się zmieni
        if (cachedArea == null || cachedFacing != facing) {
            BlockPos targetPos = pos.offset(facing);
            cachedArea = new AxisAlignedBB(targetPos).expand(0, 1, 0);
            cachedFacing = facing;
        }

        List<EntityLivingBase> mobs = world.getEntitiesWithinAABB(EntityLivingBase.class, cachedArea);

        // Adaptacyjna prędkość: jeśli brak mobów, zwalniamy sprawdzanie
        if (mobs.isEmpty()) {
            if (currentSpeed < MAX_SPEED) {
                currentSpeed += 10;
            }
        } else {
            // Są moby - maksymalna prędkość
            currentSpeed = MIN_SPEED;
        }

        boolean didKill = false;

        for (EntityLivingBase mob : mobs) {
            if (mob.isDead || mob.getHealth() <= 0) continue;
            if (mob instanceof net.minecraft.entity.player.EntityPlayer) continue; // Don't kill players

            // Use fake player to allow player-only drops
            DamageSource source = DamageSource.causePlayerDamage(getFakePlayer());

            mob.attackEntityFrom(source, 2000f); // Instakill
            didKill = true;
        }

        // Also pickup items in area (vacuum)
        List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class, cachedArea);
        for(EntityItem item : items) {
             if (item.isDead || item.getItem().isEmpty()) continue;

             ItemStack original = item.getItem();
             ItemStack remaining = insertItemToInventory(original);

             if (remaining.isEmpty()) {
                 item.setDead();
             } else if (remaining.getCount() < original.getCount()) {
                 item.setItem(remaining);
             }
        }

        if (didKill) {
            storage.extractEnergy(OPERATION_COST, false);
        }
    }

    private ItemStack insertItemToInventory(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < inventory.getSlots(); i++) {
            remaining = inventory.insertItem(i, remaining, false);
            if (remaining.isEmpty()) return ItemStack.EMPTY;
        }
        return remaining;
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
