package com.antigravity.advancedsorter.container;

import com.antigravity.advancedsorter.tiles.TileAdvancedSorter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerAdvancedSorter extends Container {
    private final TileAdvancedSorter tile;

    public ContainerAdvancedSorter(EntityPlayer player, TileAdvancedSorter tile) {
        this.tile = tile;
        // Use getInventory() for full access, not getCapability() which returns
        // insert-only wrapper
        IItemHandler inventory = tile.getInventory();

        // Tile Inventory (Buffer) - 9 slots in 1 row
        // Positioned on the right side under the buttons
        for (int i = 0; i < 9; i++) {
            addSlotToContainer(new SlotItemHandler(inventory, i, 120 + i * 18, 115));
        }

        // Player Inventory - Starts at y=138, shifted right
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlotToContainer(new Slot(player.inventory, x + y * 9 + 9, 120 + x * 18, 138 + y * 18));
            }
        }

        // Player Hotbar - at y=196, shifted right
        for (int x = 0; x < 9; x++) {
            addSlotToContainer(new Slot(player.inventory, x, 120 + x * 18, 196));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        // Basic shift-click logic
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            if (index < 9) { // From Tile to Player
                if (!this.mergeItemStack(itemstack1, 9, 45, true)) {
                    return ItemStack.EMPTY;
                }
            } else { // From Player to Tile
                if (!this.mergeItemStack(itemstack1, 0, 9, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }

        return itemstack;
    }
}
