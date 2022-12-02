package edebe.mana_box.common;

import com.mojang.blaze3d.matrix.MatrixStack;
import edebe.mana_box.ManaBox;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeColor;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ToolType;
import org.jetbrains.annotations.NotNull;
import vazkii.botania.api.wand.IWandHUD;
import vazkii.botania.api.wand.IWandable;
import vazkii.botania.common.block.BlockMod;
import vazkii.botania.common.entity.EntityManaBurst;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;

@ParametersAreNonnullByDefault
public class BlockManaBox extends BlockMod implements ITileEntityProvider, IWandHUD, IWandable {
    private static final VoxelShape SHAPE = makeCuboidShape(0, 0, 0, 16, 16, 16);

    public BlockManaBox() {
        super(AbstractBlock.Properties.create(Material.ROCK).sound(SoundType.STONE).hardnessAndResistance(2, 2000)
                .harvestLevel(1).harvestTool(ToolType.PICKAXE).setRequiresTool().notSolid().setAllowsSpawn(BlockManaBox::neverAllowSpawn)
                .setOpaque(BlockManaBox::isNotSolid).setSuffocates(BlockManaBox::isNotSolid).setBlocksVision(BlockManaBox::isNotSolid));
    }

    @OnlyIn(Dist.CLIENT)
    public boolean isSideInvisible(BlockState state, BlockState adjacentBlockState, Direction side) {
        return adjacentBlockState.getBlock() == this || super.isSideInvisible(state, adjacentBlockState, side);
    }

    @NotNull
    @Override
    public VoxelShape getRayTraceShape(BlockState state, IBlockReader reader, BlockPos pos, ISelectionContext context) {
        return VoxelShapes.empty();
    }

    @Override
    public int getOpacity(BlockState state, IBlockReader worldIn, BlockPos pos) {
        return 0;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public float getAmbientOcclusionLightValue(BlockState state, IBlockReader worldIn, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos) {
        return true;
    }

    @Nonnull
    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext ctx) {
        return SHAPE;
    }

    @NotNull
    @Override
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
        List<ItemStack> dropsOriginal = super.getDrops(state, builder);
        if (!dropsOriginal.isEmpty()) {
            return dropsOriginal;
        } else {
            ItemStack stack = new ItemStack(ManaBox.MANA_BOX_ITEM);
            TileManaBox manaBox = (TileManaBox) builder.get(LootParameters.BLOCK_ENTITY);
            BlockItemManaBox.setMana(stack, manaBox.getCurrentMana());
            BlockItemManaBox.setColor(stack, manaBox.getColor());
            return Collections.singletonList(stack);
        }
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof TileManaBox) {
            TileManaBox manaBox = (TileManaBox) tileEntity;
            manaBox.setMana(BlockItemManaBox.getBlockMana(stack));
            manaBox.setColor(BlockItemManaBox.getColor(stack));
        }
    }

    @Nonnull
    @Override
    public ActionResultType onBlockActivated(@Nonnull BlockState state, World world, @Nonnull BlockPos pos, PlayerEntity player, @Nonnull Hand hand, @Nonnull BlockRayTraceResult hit) {
        TileEntity tileEntity = world.getTileEntity(pos);
        ItemStack stack = player.getHeldItem(hand);
        if (stack.getItem() instanceof DyeItem && tileEntity instanceof TileManaBox) {
            DyeColor color = ((DyeItem) stack.getItem()).getDyeColor();
            if (color != ((TileManaBox) tileEntity).getColor()) {
                ((TileManaBox) tileEntity).setColor(color);
                stack.shrink(1);
                return ActionResultType.SUCCESS;
            }
        }
        return super.onBlockActivated(state, world, pos, player, hand, hit);
    }

    @NotNull
    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
        if (context.getEntity() instanceof EntityManaBurst) {
            return SHAPE;
        } else {
            return super.getCollisionShape(state, world, pos, context);
        }
    }

    @Nonnull
    @Override
    public TileEntity createNewTileEntity(@Nonnull IBlockReader world) {
        return new TileManaBox();
    }

    @Override
    public boolean hasComparatorInputOverride(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(BlockState state, World world, BlockPos pos) {
        TileManaBox pool = (TileManaBox) world.getTileEntity(pos);
        return TileManaBox.calculateComparatorLevel(pool.getCurrentMana(), pool.manaCap);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void renderHUD(MatrixStack matrixStack, Minecraft minecraft, World world, BlockPos pos) {
        ((TileManaBox) world.getTileEntity(pos)).renderHUD(matrixStack);
    }

    @Override
    public boolean onUsedByWand(PlayerEntity player, ItemStack stack, World world, BlockPos pos, Direction side) {
        return false;
    }

    private static Boolean neverAllowSpawn(BlockState state, IBlockReader reader, BlockPos pos, EntityType<?> entity) {
        return false;
    }

    private static boolean isNotSolid(BlockState state, IBlockReader reader, BlockPos pos) {
        return false;
    }
}
