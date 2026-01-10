package com.antigravity.advancedsorter.pipes.gas.teleport;

import com.antigravity.advancedsorter.AdvancedSorterMod;
import com.antigravity.advancedsorter.pipes.ItemWrench;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
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
import java.util.Set;

/**
 * Teleport Gas Pipe block - teleports Mekanism gases across dimensions.
 */
public class BlockTeleportGasPipe extends Block implements ITileEntityProvider {

    public static final PropertyBool NORTH = PropertyBool.create("north");
    public static final PropertyBool SOUTH = PropertyBool.create("south");
    public static final PropertyBool WEST = PropertyBool.create("west");
    public static final PropertyBool EAST = PropertyBool.create("east");
    public static final PropertyBool UP = PropertyBool.create("up");
    public static final PropertyBool DOWN = PropertyBool.create("down");

    private static final AxisAlignedBB PIPE_AABB = new AxisAlignedBB(0.25, 0.25, 0.25, 0.75, 0.75, 0.75);

    public BlockTeleportGasPipe() {
        super(Material.GLASS);
        setUnlocalizedName("teleport_gas_pipe");
        setRegistryName(AdvancedSorterMod.MODID, "teleport_gas_pipe");
        setHardness(0.5f);
        setCreativeTab(AdvancedSorterMod.CREATIVE_TAB);
        setDefaultState(blockState.getBaseState()
                .withProperty(NORTH, false)
                .withProperty(SOUTH, false)
                .withProperty(WEST, false)
                .withProperty(EAST, false)
                .withProperty(UP, false)
                .withProperty(DOWN, false));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.DARK_PURPLE + "Teleports gases across dimensions");
        tooltip.add(TextFormatting.GRAY + "Right-click to configure frequency");
        if (net.minecraft.client.gui.GuiScreen.isShiftKeyDown()) {
            tooltip.add(TextFormatting.YELLOW + "Works with Mekanism gases");
            tooltip.add(TextFormatting.YELLOW + "Set same frequency on multiple pipes to link them");
        } else {
            tooltip.add(TextFormatting.DARK_GRAY + "Hold SHIFT for more info");
        }
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileTeleportGasPipe();
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
                if (te instanceof TileTeleportGasPipe) {
                    ((TileTeleportGasPipe) te).toggleConnection(facing);
                    return true;
                }
            }

            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileTeleportGasPipe) {
                // Open GUI (GUI ID 11 for teleport gas pipe)
                playerIn.openGui(AdvancedSorterMod.instance, AdvancedSorterMod.GUI_TELEPORT_GAS_PIPE,
                        worldIn, pos.getX(), pos.getY(), pos.getZ());
                return true;
            }
        }
        return true;
    }

    private boolean isWrench(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        if (stack.getItem() instanceof ItemWrench)
            return true;
        String registryName = stack.getItem().getRegistryName().toString().toLowerCase();
        return registryName.contains("wrench");
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileTeleportGasPipe) {
                ((TileTeleportGasPipe) te).checkConnections();
            }
        }
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileTeleportGasPipe) {
                ((TileTeleportGasPipe) te).checkConnections();
            }
            notifyNeighborPipes(worldIn, pos);
        }
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        super.breakBlock(worldIn, pos, state);
        notifyNeighborPipes(worldIn, pos);
    }

    private void notifyNeighborPipes(World world, BlockPos pos) {
        for (EnumFacing face : EnumFacing.VALUES) {
            BlockPos neighborPos = pos.offset(face);
            TileEntity te = world.getTileEntity(neighborPos);
            if (te instanceof TileTeleportGasPipe) {
                ((TileTeleportGasPipe) te).checkConnections();
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
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        TileEntity tile = worldIn.getTileEntity(pos);
        if (tile instanceof TileTeleportGasPipe) {
            Set<EnumFacing> connections = ((TileTeleportGasPipe) tile).getConnections();
            return state.withProperty(NORTH, connections.contains(EnumFacing.NORTH))
                    .withProperty(SOUTH, connections.contains(EnumFacing.SOUTH))
                    .withProperty(WEST, connections.contains(EnumFacing.WEST))
                    .withProperty(EAST, connections.contains(EnumFacing.EAST))
                    .withProperty(UP, connections.contains(EnumFacing.UP))
                    .withProperty(DOWN, connections.contains(EnumFacing.DOWN));
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
}
