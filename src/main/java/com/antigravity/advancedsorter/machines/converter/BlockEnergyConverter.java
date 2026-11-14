package com.antigravity.advancedsorter.machines.converter;

import com.antigravity.advancedsorter.AdvancedSorterMod;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Universal Energy Converter - converts between different energy systems without loss.
 *
 * Supported systems:
 * - RF/FE (Forge Energy)
 * - Mekanism Joules
 * - IC2 EU (if IC2 is installed)
 *
 * Conversion rates:
 * - 1 RF = 1 FE
 * - 1 RF = 2.5 Joules
 * - 1 EU = 4 RF
 */
public class BlockEnergyConverter extends Block implements ITileEntityProvider {

    public BlockEnergyConverter() {
        super(Material.IRON);
        setUnlocalizedName(AdvancedSorterMod.MODID + ".energy_converter");
        setRegistryName("energy_converter");
        setCreativeTab(AdvancedSorterMod.CREATIVE_TAB);
        setHardness(3.0f);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GRAY + "Converts energy between different systems.");
        tooltip.add(TextFormatting.GREEN + "RF/FE " + TextFormatting.WHITE + "<->" + TextFormatting.AQUA + " Mekanism Joules");
        tooltip.add(TextFormatting.GREEN + "RF/FE " + TextFormatting.WHITE + "<->" + TextFormatting.YELLOW + " IC2 EU");
        tooltip.add(TextFormatting.DARK_GRAY + "Lossless conversion!");
        if (net.minecraft.client.gui.GuiScreen.isShiftKeyDown()) {
            tooltip.add("");
            tooltip.add(TextFormatting.AQUA + "Conversion Rates:");
            tooltip.add(TextFormatting.WHITE + "  1 RF = 1 FE");
            tooltip.add(TextFormatting.WHITE + "  1 RF = 2.5 Joules");
            tooltip.add(TextFormatting.WHITE + "  1 EU = 4 RF");
        } else {
            tooltip.add(TextFormatting.DARK_GRAY + "Hold SHIFT for conversion rates.");
        }
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEnergyConverter();
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, net.minecraft.util.math.BlockPos pos, Block blockIn, net.minecraft.util.math.BlockPos fromPos) {
        super.neighborChanged(state, worldIn, pos, blockIn, fromPos);
        TileEntity tile = worldIn.getTileEntity(pos);
        if (tile instanceof TileEnergyConverter) {
            ((TileEnergyConverter) tile).onNeighborChanged();
        }
    }

    @Override
    public boolean onBlockActivated(World worldIn, net.minecraft.util.math.BlockPos pos, IBlockState state, EntityPlayer playerIn,
            net.minecraft.util.EnumHand hand, net.minecraft.util.EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity tile = worldIn.getTileEntity(pos);
            if (tile instanceof TileEnergyConverter) {
                TileEnergyConverter converter = (TileEnergyConverter) tile;
                int stored = converter.getEnergyStored();
                int max = converter.getMaxEnergyStored();
                playerIn.sendMessage(new net.minecraft.util.text.TextComponentString(
                        TextFormatting.YELLOW + "Energy: " + TextFormatting.WHITE + stored + " / " + max + " RF"));
            }
        }
        return true;
    }
}
