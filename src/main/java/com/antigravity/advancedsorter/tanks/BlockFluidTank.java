package com.antigravity.advancedsorter.tanks;

import com.antigravity.advancedsorter.AdvancedSorterMod;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class BlockFluidTank extends Block implements ITileEntityProvider {

    public static final PropertyEnum<TankTier> TIER = PropertyEnum.create("tier", TankTier.class);

    public BlockFluidTank() {
        super(Material.GLASS);
        setUnlocalizedName(AdvancedSorterMod.MODID + ".fluid_tank");
        setRegistryName("fluid_tank");
        setCreativeTab(AdvancedSorterMod.CREATIVE_TAB);
        setHardness(3.5f);
        setResistance(10.0f);
        setDefaultState(blockState.getBaseState().withProperty(TIER, TankTier.BASIC));
        setLightOpacity(0);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, TIER);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(TIER).getId();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(TIER, TankTier.fromMeta(meta));
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
        for (TankTier tier : TankTier.values()) {
            items.add(new ItemStack(this, 1, tier.getId()));
        }
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(TIER).getId();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        TankTier tier = TankTier.fromMeta(stack.getMetadata());

        // Show stored fluid if any
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey("TileData")) {
            NBTTagCompound tileData = tag.getCompoundTag("TileData");
            if (tileData.hasKey("Tank")) {
                NBTTagCompound tankData = tileData.getCompoundTag("Tank");
                FluidStack fluid = FluidStack.loadFluidStackFromNBT(tankData);
                if (fluid != null && fluid.amount > 0) {
                    tooltip.add(TextFormatting.AQUA + fluid.getLocalizedName() + ": " +
                            formatAmount(fluid.amount) + " / " + formatAmount(tier.getCapacity()));
                }
            }
        }

        tooltip.add(TextFormatting.GRAY + "Capacity: " + formatAmount(tier.getCapacity()));
        tooltip.add(TextFormatting.GRAY + "Transfer: " + tier.getTransferRate() + " mB/t");
        tooltip.add(TextFormatting.DARK_GRAY + "Right-click to configure sides");
    }

    private String formatAmount(int amount) {
        if (amount >= 1000) {
            return String.format("%.1fB", amount / 1000.0);
        }
        return amount + "mB";
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
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return true;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileFluidTank(TankTier.fromMeta(meta));
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);

        // Restore fluid from item NBT
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileFluidTank && stack.hasTagCompound()) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag.hasKey("TileData")) {
                ((TileFluidTank) te).readFromItemNBT(tag.getCompoundTag("TileData"));
            }
        }
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        // Drop item with fluid data
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileFluidTank && !worldIn.isRemote) {
            TileFluidTank tank = (TileFluidTank) te;
            ItemStack drop = new ItemStack(this, 1, state.getValue(TIER).getId());

            if (tank.getTank().getFluidAmount() > 0) {
                NBTTagCompound tag = new NBTTagCompound();
                NBTTagCompound tileData = new NBTTagCompound();
                tank.writeToItemNBT(tileData);
                tag.setTag("TileData", tileData);
                drop.setTagCompound(tag);
            }

            spawnAsEntity(worldIn, pos, drop);
        }

        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        if (willHarvest) {
            return true; // Delay removal until getDrops is called
        }
        return super.removedByPlayer(state, world, pos, player, willHarvest);
    }

    @Override
    public void harvestBlock(World worldIn, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack stack) {
        super.harvestBlock(worldIn, player, pos, state, te, stack);
        worldIn.setBlockToAir(pos);
    }

    @Override
    public java.util.List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        // Return empty list - we handle drops manually in breakBlock
        return java.util.Collections.emptyList();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {

        // Try bucket interaction first
        ItemStack heldItem = playerIn.getHeldItem(hand);
        if (!heldItem.isEmpty()) {
            // Check if it's a fluid container
            if (FluidUtil.getFluidHandler(heldItem) != null) {
                TileEntity te = worldIn.getTileEntity(pos);
                if (te != null && te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null)) {
                    IFluidHandler handler = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
                    if (handler != null) {
                        // Smart interaction logic:
                        // If item has fluid -> Try to empty it into tank FIRST
                        // If item is empty -> Try to fill it from tank
                        FluidStack fluidInItem = FluidUtil.getFluidContained(heldItem);
                        boolean success = false;

                        if (fluidInItem != null && fluidInItem.amount > 0) {
                            // Item has fluid: Try to empty item -> tank
                            net.minecraftforge.fluids.FluidActionResult result = FluidUtil.tryEmptyContainer(heldItem, handler, Integer.MAX_VALUE, playerIn, true);
                            if (result.isSuccess()) {
                                if (heldItem.getCount() == 1) {
                                    playerIn.setHeldItem(hand, result.getResult());
                                } else {
                                    heldItem.shrink(1);
                                    if (!playerIn.addItemStackToInventory(result.getResult())) {
                                        playerIn.dropItem(result.getResult(), false);
                                    }
                                }
                                success = true;
                            }
                        } else {
                            // Item is empty: Try to fill item <- tank
                            net.minecraftforge.fluids.FluidActionResult result = FluidUtil.tryFillContainer(heldItem, handler, Integer.MAX_VALUE, playerIn, true);
                            if (result.isSuccess()) {
                                if (heldItem.getCount() == 1) {
                                    playerIn.setHeldItem(hand, result.getResult());
                                } else {
                                    heldItem.shrink(1);
                                    if (!playerIn.addItemStackToInventory(result.getResult())) {
                                        playerIn.dropItem(result.getResult(), false);
                                    }
                                }
                                success = true;
                            }
                        }

                        if (success) return true;
                    }
                }
            }
        }

        // Open GUI if bucket interaction didn't happen
        if (!worldIn.isRemote) {
            playerIn.openGui(AdvancedSorterMod.instance, AdvancedSorterMod.GUI_ADVANCED_TANK, worldIn, pos.getX(),
                    pos.getY(), pos.getZ());
        }
        return true;
    }
}
