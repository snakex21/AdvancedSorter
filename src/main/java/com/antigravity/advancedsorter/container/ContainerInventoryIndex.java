package com.antigravity.advancedsorter.container;

import com.antigravity.advancedsorter.tiles.TileInventoryIndex;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerInventoryIndex extends Container {

    private final TileInventoryIndex tile;

    public ContainerInventoryIndex(InventoryPlayer playerInv, TileInventoryIndex tile) {
        this.tile = tile;

        IItemHandler handler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);

        // Position of the Output Buffer slots (3x3 grid)
        // These will be positioned in the GET tab area (will be moved by GUI)
        int xPos = 133; // Right side for GET tab
        int yPos = 54;
        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 3; ++x) {
                this.addSlotToContainer(new SlotItemHandler(handler, x + y * 3, xPos + x * 18, yPos + y * 18));
            }
        }

        // Player Inventory (Aligned with ySize=256)
        // Texture starts at 170, first row at 188
        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 9; ++x) {
                this.addSlotToContainer(new Slot(playerInv, x + y * 9 + 9, 12 + x * 18, 188 + y * 18));
            }
        }

        // Hotbar (At the bottom)
        for (int x = 0; x < 9; ++x) {
            this.addSlotToContainer(new Slot(playerInv, x, 12 + x * 18, 246));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return !playerIn.isSpectator();
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            // Slots 0-8 are TileEntity slots (Output)
            // Slots 9-35 are Inventory
            // Slots 36-44 are Hotbar

            if (index < 9) {
                // From Output to Player Inventory
                if (!this.mergeItemStack(itemstack1, 9, 45, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Player clicked item in their inventory.
                // We usually don't insert INTO the output buffer manually.
                // But if they shift click, maybe we just do nothing or try to insert?
                // For now, let's allow inserting into output buffer if there's space (why not?)
                if (!this.mergeItemStack(itemstack1, 0, 9, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, itemstack1);
        }

        return itemstack;
    }
}
