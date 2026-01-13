package com.antigravity.advancedsorter.autocrafter;

import com.antigravity.advancedsorter.AdvancedSorterMod;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Auto Crafter Block - automatyczny crafter z wlasnymi recepturami.
 *
 * Funkcje:
 * - Definiowanie wlasnych receptur (mapowanie przedmiotow)
 * - Lancuchowe craftowanie (jesli brakuje skladnika, craftuje go najpierw)
 * - 3 zakladki: Receptury, Craftowanie, Automatyzacja
 * - Integracja z rurami do automatyzacji
 * - Wizualizacja wyniku receptury w czasie rzeczywistym
 */
public class BlockAutoCrafter extends Block implements ITileEntityProvider {

    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static BlockAutoCrafter INSTANCE;

    public BlockAutoCrafter() {
        super(Material.IRON);
        setUnlocalizedName(AdvancedSorterMod.MODID + ".auto_crafter");
        setRegistryName("auto_crafter");
        setCreativeTab(AdvancedSorterMod.CREATIVE_TAB);
        setHardness(3.5f);
        setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
        INSTANCE = this;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GRAY + "Automatyczny crafter z wlasnymi recepturami");
        if (net.minecraft.client.gui.GuiScreen.isShiftKeyDown()) {
            tooltip.add(TextFormatting.AQUA + "Funkcje:");
            tooltip.add(TextFormatting.YELLOW + "- Definiuj wlasne receptury");
            tooltip.add(TextFormatting.YELLOW + "- Lancuchowe craftowanie");
            tooltip.add(TextFormatting.YELLOW + "- Automatyzacja z rurami");
            tooltip.add(TextFormatting.YELLOW + "- Wizualizacja wyniku");
        } else {
            tooltip.add(TextFormatting.DARK_GRAY + "Przytrzymaj SHIFT po wiecej info");
        }
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileAutoCrafter();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            playerIn.openGui(AdvancedSorterMod.instance, AdvancedSorterMod.GUI_AUTO_CRAFTER,
                    worldIn, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity tile = worldIn.getTileEntity(pos);
        if (tile instanceof TileAutoCrafter) {
            TileAutoCrafter crafter = (TileAutoCrafter) tile;

            // Drop all items from input slots
            IItemHandler inputHandler = crafter.getInputHandler();
            if (inputHandler != null) {
                for (int i = 0; i < inputHandler.getSlots(); i++) {
                    ItemStack stack = inputHandler.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        InventoryHelper.spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
            }

            // Drop all items from output slots
            IItemHandler outputHandler = crafter.getOutputHandler();
            if (outputHandler != null) {
                for (int i = 0; i < outputHandler.getSlots(); i++) {
                    ItemStack stack = outputHandler.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        InventoryHelper.spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
            }
        }
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing facing = EnumFacing.getHorizontal(meta);
        if (facing.getAxis() == EnumFacing.Axis.Y) {
            facing = EnumFacing.NORTH;
        }
        return this.getDefaultState().withProperty(FACING, facing);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }
}
