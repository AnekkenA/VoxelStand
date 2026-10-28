package com.voxelstand.block;

import com.mojang.serialization.MapCodec;
import com.voxelstand.block.entity.StandPartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class StandPartBlock extends BaseEntityBlock {
    public static final MapCodec<StandPartBlock> CODEC = simpleCodec(StandPartBlock::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public StandPartBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public StandPartBlock() {
        this(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5f)
                .sound(SoundType.WOOD)
                .noOcclusion()
                .isViewBlocking((s, l, p) -> false));
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return false;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.box(0.0, 0.0, 0.0, 1.0, 0.95, 1.0);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.box(0.0, 0.0, 0.0, 1.0, 0.95, 1.0);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StandPartBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof StandPartBlockEntity part) {
            BlockPos master = part.getMasterPos();
            if (master != null && !master.equals(BlockPos.ZERO)) {
                BlockState masterState = level.getBlockState(master);
                return masterState.useItemOn(held, level, player, hand, hit.withPosition(master));
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof StandPartBlockEntity part) {
            BlockPos master = part.getMasterPos();
            if (master != null && !master.equals(BlockPos.ZERO)) {
                BlockState masterState = level.getBlockState(master);
                return masterState.useWithoutItem(level, player, hit.withPosition(master));
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof StandPartBlockEntity part) {
            BlockPos master = part.getMasterPos();
            if (master != null && !master.equals(BlockPos.ZERO)) {
                BlockState masterState = level.getBlockState(master);
                masterState.attack(level, master, player);
                return;
            }
        }
        super.attack(state, level, pos, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof StandPartBlockEntity part) {
                    BlockPos master = part.getMasterPos();
                    if (master != null && !master.equals(BlockPos.ZERO) && !master.equals(pos)) {
                        BlockState masterState = level.getBlockState(master);
                        if (!masterState.isAir()) {
                            level.destroyBlock(master, true);
                        }
                    }
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof StandPartBlockEntity part) {
                BlockPos master = part.getMasterPos();
                if (master != null && !master.equals(BlockPos.ZERO) && !master.equals(pos)) {
                    BlockState masterState = level.getBlockState(master);
                    if (!masterState.isAir()) {
                        level.destroyBlock(master, !player.isCreative());
                    }
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof StandPartBlockEntity part) {
            BlockPos master = part.getMasterPos();
            if (master != null && !master.equals(BlockPos.ZERO)) {
                BlockState masterState = level.getBlockState(master);
                return masterState.getBlock().getCloneItemStack(level, master, masterState);
            }
        }
        return new ItemStack(ModBlocks.SCULPT_STAND.get());
    }
}
