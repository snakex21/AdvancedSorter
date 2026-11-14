package com.antigravity.advancedsorter.machines.obsidiangen;

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
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class BlockObsidianGenerator extends Block implements ITileEntityProvider {

    public BlockObsidianGenerator() {
        super(Material.IRON);
        setUnlocalizedName(AdvancedSorterMod.MODID + ".obsidian_generator");
        setRegistryName("obsidian_generator");
        setCreativeTab(AdvancedSorterMod.CREATIVE_TAB);
        setHardness(3.0f);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GRAY + "Generates Obsidian using Energy.");
        tooltip.add(TextFormatting.RED + "Consumes Water and Lava!");
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileObsidianGenerator();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            if (FluidUtil.interactWithFluidHandler(playerIn, hand, worldIn, pos, facing)) {
                return true;
            }
            playerIn.openGui(AdvancedSorterMod.instance, AdvancedSorterMod.GUI_OBSIDIAN_GENERATOR, worldIn, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }
}
