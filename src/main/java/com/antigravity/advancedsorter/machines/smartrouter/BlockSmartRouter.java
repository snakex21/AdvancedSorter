package com.antigravity.advancedsorter.machines.smartrouter;

import com.antigravity.advancedsorter.AdvancedSorterMod;
import com.antigravity.advancedsorter.client.gui.GuiHandler;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockSmartRouter extends Block implements ITileEntityProvider {

    public BlockSmartRouter() {
        super(Material.IRON);
        setUnlocalizedName("smart_router");
        setRegistryName("smart_router");
        setCreativeTab(AdvancedSorterMod.CREATIVE_TAB);
        setHardness(3.0f);
        setResistance(15.0f);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            playerIn.openGui(AdvancedSorterMod.instance, AdvancedSorterMod.GUI_SMART_ROUTER, worldIn, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity tileentity = worldIn.getTileEntity(pos);
        if (tileentity instanceof TileSmartRouter) {
            TileSmartRouter tile = (TileSmartRouter) tileentity;

            // Drop automation inventory
            net.minecraftforge.items.IItemHandler inventory = tile.getCapability(net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (inventory != null) {
                for (int i = 0; i < inventory.getSlots(); i++) {
                    ItemStack stack = inventory.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        InventoryHelper.spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
            }

            // Drop manual sending inventory
            net.minecraftforge.items.IItemHandler manualInv = tile.getManualInventory();
            if (manualInv != null) {
                for (int i = 0; i < manualInv.getSlots(); i++) {
                    ItemStack stack = manualInv.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        InventoryHelper.spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
            }
        }
        super.breakBlock(worldIn, pos, state);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileSmartRouter();
    }
}
