package com.antigravity.advancedsorter.blocks;

import com.antigravity.advancedsorter.AdvancedSorterMod;
import com.antigravity.advancedsorter.tiles.TileAdvancedSorter;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class BlockAdvancedSorter extends Block implements ITileEntityProvider {
    public static BlockAdvancedSorter INSTANCE;

    public BlockAdvancedSorter() {
        super(Material.IRON);
        setUnlocalizedName(AdvancedSorterMod.MODID + ".advanced_sorter");
        setRegistryName("advanced_sorter");
        setCreativeTab(AdvancedSorterMod.CREATIVE_TAB);
        setHardness(3.0f);
        INSTANCE = this;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GRAY
                + net.minecraft.client.resources.I18n.format("tooltip.advancedsorter.advanced_sorter.desc"));
        if (net.minecraft.client.gui.GuiScreen.isShiftKeyDown()) {
            tooltip.add(TextFormatting.AQUA
                    + net.minecraft.client.resources.I18n.format("tooltip.advancedsorter.shift_info"));
            tooltip.add(TextFormatting.YELLOW
                    + net.minecraft.client.resources.I18n.format("tooltip.advancedsorter.advanced_sorter.details"));
        } else {
            tooltip.add(TextFormatting.DARK_GRAY
                    + net.minecraft.client.resources.I18n.format("tooltip.advancedsorter.hold_shift"));
        }
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileAdvancedSorter();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            playerIn.openGui(AdvancedSorterMod.instance, com.antigravity.advancedsorter.client.gui.GuiHandler.GUI_ID,
                    worldIn, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }
}
