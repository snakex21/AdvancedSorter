package com.antigravity.advancedsorter.pipes.fluid;

import com.antigravity.advancedsorter.AdvancedSorterMod;
import com.antigravity.advancedsorter.pipes.ItemWrench;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Base block class for fluid transport pipes.
 */
public class BlockFluidPipe extends Block implements ITileEntityProvider {

    public static final PropertyBool NORTH = PropertyBool.create("north");
    public static final PropertyBool SOUTH = PropertyBool.create("south");
    public static final PropertyBool EAST = PropertyBool.create("east");
    public static final PropertyBool WEST = PropertyBool.create("west");
    public static final PropertyBool UP = PropertyBool.create("up");
    public static final PropertyBool DOWN = PropertyBool.create("down");

    private static final AxisAlignedBB PIPE_AABB = new AxisAlignedBB(0.25, 0.25, 0.25, 0.75, 0.75, 0.75);

    protected final FluidPipeTier tier;

    public BlockFluidPipe() {
        this("iron_fluid_pipe", FluidPipeTier.IRON);
    }

    protected BlockFluidPipe(String name, FluidPipeTier tier) {
        super(Material.GLASS);
        this.tier = tier;
        setUnlocalizedName(name);
        setRegistryName(AdvancedSorterMod.MODID, name);
        setHardness(0.5f);
        setCreativeTab(AdvancedSorterMod.CREATIVE_TAB);
    }

    public FluidPipeTier getTier() {
        return tier;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GRAY
                + net.minecraft.client.resources.I18n.format("tooltip.advancedsorter.fluid_pipe.desc"));
        if (net.minecraft.client.gui.GuiScreen.isShiftKeyDown()) {
            tooltip.add(TextFormatting.AQUA
                    + net.minecraft.client.resources.I18n.format("tooltip.advancedsorter.shift_info"));
            tooltip.add(TextFormatting.YELLOW
                    + net.minecraft.client.resources.I18n.format("tooltip.advancedsorter.fluid_pipe.details"));
        } else {
            tooltip.add(TextFormatting.DARK_GRAY
                    + net.minecraft.client.resources.I18n.format("tooltip.advancedsorter.hold_shift"));
        }
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileFluidPipe) {
            TileFluidPipe pipe = (TileFluidPipe) te;
            return state
                    .withProperty(NORTH, pipe.getConnections().contains(EnumFacing.NORTH))
                    .withProperty(SOUTH, pipe.getConnections().contains(EnumFacing.SOUTH))
                    .withProperty(EAST, pipe.getConnections().contains(EnumFacing.EAST))
                    .withProperty(WEST, pipe.getConnections().contains(EnumFacing.WEST))
                    .withProperty(UP, pipe.getConnections().contains(EnumFacing.UP))
                    .withProperty(DOWN, pipe.getConnections().contains(EnumFacing.DOWN));
        }
        return state;
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState();
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileFluidPipe(tier);
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileFluidPipe) {
                ((TileFluidPipe) te).updateConnections();
            }
            notifyNeighborPipes(worldIn, pos);
        }
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        super.breakBlock(worldIn, pos, state);
        notifyNeighborPipes(worldIn, pos);
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileFluidPipe) {
                ((TileFluidPipe) te).markConnectionsDirty();
            }
        }
    }

    private void notifyNeighborPipes(World world, BlockPos pos) {
        for (EnumFacing face : EnumFacing.VALUES) {
            BlockPos neighborPos = pos.offset(face);
            TileEntity te = world.getTileEntity(neighborPos);
            if (te instanceof TileFluidPipe) {
                ((TileFluidPipe) te).updateConnections();
            }
        }
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return PIPE_AABB;
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (playerIn.isSneaking())
            return false;

        if (!worldIn.isRemote) {
            ItemStack held = playerIn.getHeldItem(hand);
            if (isWrench(held)) {
                TileEntity te = worldIn.getTileEntity(pos);
                if (te instanceof TileFluidPipe) {
                    ((TileFluidPipe) te).toggleConnection(facing);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isWrench(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        if (stack.getItem() instanceof ItemWrench)
            return true;
        String registryName = stack.getItem().getRegistryName().toString().toLowerCase();
        return registryName.contains("wrench");
    }
}
