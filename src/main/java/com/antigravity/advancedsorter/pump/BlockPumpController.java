package com.antigravity.advancedsorter.pump;

import com.antigravity.advancedsorter.AdvancedSorterMod;
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

/**
 * Pump controller block - controls extraction pipes remotely by frequency.
 */
public class BlockPumpController extends Block implements ITileEntityProvider {

    public BlockPumpController() {
        super(Material.IRON);
        setUnlocalizedName("pump_controller");
        setRegistryName(AdvancedSorterMod.MODID, "pump_controller");
        setHardness(2.0f);
        setCreativeTab(AdvancedSorterMod.CREATIVE_TAB);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GRAY
                + net.minecraft.client.resources.I18n.format("tooltip.advancedsorter.pump_controller.desc"));
        if (net.minecraft.client.gui.GuiScreen.isShiftKeyDown()) {
            tooltip.add(TextFormatting.AQUA
                    + net.minecraft.client.resources.I18n.format("tooltip.advancedsorter.shift_info"));
            tooltip.add(TextFormatting.YELLOW
                    + net.minecraft.client.resources.I18n.format("tooltip.advancedsorter.pump_controller.details"));
        } else {
            tooltip.add(TextFormatting.DARK_GRAY
                    + net.minecraft.client.resources.I18n.format("tooltip.advancedsorter.hold_shift"));
        }
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TilePumpController();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TilePumpController) {
                playerIn.openGui(AdvancedSorterMod.instance, AdvancedSorterMod.GUI_PUMP_CONTROLLER,
                        worldIn, pos.getX(), pos.getY(), pos.getZ());
                return true;
            }
        }
        return true;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TilePumpController) {
            te.invalidate();
        }
        super.breakBlock(worldIn, pos, state);
    }
}
