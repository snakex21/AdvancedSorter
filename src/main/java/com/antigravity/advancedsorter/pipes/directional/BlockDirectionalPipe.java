package com.antigravity.advancedsorter.pipes.directional;

import com.antigravity.advancedsorter.AdvancedSorterMod;
import com.antigravity.advancedsorter.pipes.BlockItemPipe;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Directional Pipe Block - forces item flow in configurable directions.
 * Right-click opens GUI to configure which sides are INPUT, OUTPUT, or
 * DISABLED.
 * Colors: GREEN=INPUT, RED=OUTPUT, GRAY=DISABLED
 */
public class BlockDirectionalPipe extends BlockItemPipe {

    // Mode properties for each side (0=DISABLED, 1=INPUT, 2=OUTPUT)
    public static final PropertyInteger MODE_NORTH = PropertyInteger.create("mode_north", 0, 2);
    public static final PropertyInteger MODE_SOUTH = PropertyInteger.create("mode_south", 0, 2);
    public static final PropertyInteger MODE_EAST = PropertyInteger.create("mode_east", 0, 2);
    public static final PropertyInteger MODE_WEST = PropertyInteger.create("mode_west", 0, 2);
    public static final PropertyInteger MODE_UP = PropertyInteger.create("mode_up", 0, 2);
    public static final PropertyInteger MODE_DOWN = PropertyInteger.create("mode_down", 0, 2);

    public BlockDirectionalPipe() {
        super("directional_pipe");
        setDefaultState(getDefaultState()
                .withProperty(MODE_NORTH, 0)
                .withProperty(MODE_SOUTH, 0)
                .withProperty(MODE_EAST, 0)
                .withProperty(MODE_WEST, 0)
                .withProperty(MODE_UP, 0)
                .withProperty(MODE_DOWN, 0));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this,
                NORTH, SOUTH, EAST, WEST, UP, DOWN,
                MODE_NORTH, MODE_SOUTH, MODE_EAST, MODE_WEST, MODE_UP, MODE_DOWN);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        // First get connection state from parent
        state = super.getActualState(state, worldIn, pos);

        // Then add mode properties
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileDirectionalPipe) {
            TileDirectionalPipe tile = (TileDirectionalPipe) te;
            state = state
                    .withProperty(MODE_NORTH, tile.getSideMode(EnumFacing.NORTH).getId())
                    .withProperty(MODE_SOUTH, tile.getSideMode(EnumFacing.SOUTH).getId())
                    .withProperty(MODE_EAST, tile.getSideMode(EnumFacing.EAST).getId())
                    .withProperty(MODE_WEST, tile.getSideMode(EnumFacing.WEST).getId())
                    .withProperty(MODE_UP, tile.getSideMode(EnumFacing.UP).getId())
                    .withProperty(MODE_DOWN, tile.getSideMode(EnumFacing.DOWN).getId());
        }
        return state;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileDirectionalPipe();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        // If player is holding a placeable block (not a pipe), let them place it
        net.minecraft.item.ItemStack held = playerIn.getHeldItem(hand);
        if (!held.isEmpty() && held.getItem() instanceof net.minecraft.item.ItemBlock) {
            net.minecraft.item.ItemBlock itemBlock = (net.minecraft.item.ItemBlock) held.getItem();
            // If not a pipe, allow normal block placement
            if (!(itemBlock.getBlock() instanceof BlockItemPipe)) {
                return false; // Let vanilla handle block placement
            }
        }

        // Open GUI for configuration
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileDirectionalPipe) {
                playerIn.openGui(AdvancedSorterMod.instance, AdvancedSorterMod.GUI_DIRECTIONAL_PIPE, worldIn,
                        pos.getX(), pos.getY(), pos.getZ());
                return true;
            }
        }
        return true;
    }
}
