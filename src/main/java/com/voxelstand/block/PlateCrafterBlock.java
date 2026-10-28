package com.voxelstand.block;

import com.voxelstand.block.entity.ModBlockEntities;
import com.voxelstand.block.entity.PlateCrafterBlockEntity;
import com.voxelstand.client.DynamicMaterialSampler;
import com.voxelstand.item.PlateItem;
import com.voxelstand.item.PlateQuality;
import com.voxelstand.item.PlateSize;
import com.voxelstand.voxel.ModdedMaterialCatalog;
import com.voxelstand.voxel.VoxelMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Нарезчик пластин (Minecraft 1.21.1 NeoForge).
 * Нарезает слитки, самоцветы и ресурсы в неисследованные пластины.
 * Полностью совместим со сборками на ~800+ модов:
 * - Поддерживает ванильные материалы
 * - Поддерживает модовые материалы из ModdedMaterialCatalog (Create, Thermal, Mekanism, etc.)
 * - Поддерживает любые сторонние предметы с тегами #c:ingots, #c:gems, #c:raw_materials с динамическим семплированием цвета центрального пикселя!
 */
import com.voxelstand.block.entity.StandPartBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

public class PlateCrafterBlock extends BaseEntityBlock {
    public static final com.mojang.serialization.MapCodec<PlateCrafterBlock> CODEC = simpleCodec(PlateCrafterBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    private static PlateSize currentSelectedSize = PlateSize.SIZE_16x16;

    public PlateCrafterBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public PlateCrafterBlock() {
        this(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5F)
                .sound(SoundType.WOOD));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        Direction right = facing.getClockWise();
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();

        BlockPos pRight = pos.relative(right);
        if (!level.getBlockState(pRight).canBeReplaced(context)) {
            return null;
        }

        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            Direction right = facing.getClockWise();
            BlockPos pRight = pos.relative(right);

            level.setBlock(pRight, ModBlocks.STAND_PART.get().defaultBlockState(), 3);
            BlockEntity be = level.getBlockEntity(pRight);
            if (be instanceof StandPartBlockEntity part) {
                part.setMasterPos(pos);
            }
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            Direction right = facing.getClockWise();
            BlockPos pRight = pos.relative(right);
            BlockState partState = level.getBlockState(pRight);
            if (partState.is(ModBlocks.STAND_PART.get())) {
                level.setBlock(pRight, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlateCrafterBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? createTickerHelper(type, ModBlockEntities.PLATE_CRAFTER.get(), PlateCrafterBlockEntity::clientTick) : null;
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Shift + ПКМ: переключение размера нарезаемых пластин
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                PlateSize[] sizes = PlateSize.values();
                int nextIndex = (currentSelectedSize.ordinal() + 1) % sizes.length;
                currentSelectedSize = sizes[nextIndex];
                player.displayClientMessage(
                        Component.literal("§e[Нарезчик] Выбран размер: §f" + currentSelectedSize.getName() +
                                " §7(выход: " + currentSelectedSize.getCraftYield() + " шт., расход: " + currentSelectedSize.getIngredientCost() + " слиток)"), true);
                level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.5f, 1.2f);
            }
            return ItemInteractionResult.SUCCESS;
        }

        // Клик пластиной по нарезчику: подсказка по осмотру через подзорную трубу
        if (held.getItem() instanceof PlateItem) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PlateCrafterBlockEntity crafter) {
                crafter.triggerSpin();
            }

            if (!level.isClientSide) {
                if (!PlateItem.hasQuality(held)) {
                    player.displayClientMessage(Component.literal("§e[Нарезчик] Для изучения качества пластины возьмите подзорную трубу в левую руку и зажмите ПКМ."), true);
                } else {
                    PlateQuality quality = PlateItem.getQuality(held);
                    player.displayClientMessage(Component.literal("§a[Нарезчик] Пластина уже изучена: " + (quality != null ? quality.getDisplayName() : "") + " качество."), true);
                }
                level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.4f, 1.4f);
            }
            return ItemInteractionResult.SUCCESS;
        }

        // Нарезка пластин из ресурсов
        if (!held.isEmpty()) {
            // 1. Проверяем ванильные материалы
            VoxelMaterial mat = findMaterialForIngredient(held);
            if (mat != null && mat != VoxelMaterial.AIR) {
                return executeCutVanilla(held, mat, level, pos, player);
            }

            // 2. Проверяем модовые материалы (ModdedMaterialCatalog + теги #c:ingots / #c:gems)
            ModdedMaterialCatalog.ModdedMaterial moddedMat = ModdedMaterialCatalog.resolve(held);
            if (moddedMat != null) {
                return executeCutModded(held, moddedMat, level, pos, player);
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // Ручной толчок маховика пустой рукой (тактильный отклик)
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PlateCrafterBlockEntity crafter) {
            crafter.triggerSpin();
            level.playSound(player, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.5f, 1.3f);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private ItemInteractionResult executeCutVanilla(ItemStack held, VoxelMaterial mat, Level level, BlockPos pos, Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PlateCrafterBlockEntity crafter) {
            crafter.triggerSpin();
        }

        if (!level.isClientSide) {
            int cost = currentSelectedSize.getIngredientCost();
            if (held.getCount() < cost) {
                player.displayClientMessage(
                        Component.literal("§cДля размера " + currentSelectedSize.getName() + " требуется " + cost + " ед. материала!"), true);
                level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.6f, 1.0f);
                return ItemInteractionResult.FAIL;
            }

            held.shrink(cost);
            ItemStack plates = PlateItem.create(mat, currentSelectedSize, currentSelectedSize.getCraftYield());
            if (!player.addItem(plates)) {
                player.drop(plates, false);
            }
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.7f, 1.4f);
            player.displayClientMessage(
                    Component.literal("§aНарезано " + currentSelectedSize.getCraftYield() + " неисследованных пластин (" + currentSelectedSize.getName() + ") из " + mat.getName()), true);
        }
        return ItemInteractionResult.SUCCESS;
    }

    private ItemInteractionResult executeCutModded(ItemStack held, ModdedMaterialCatalog.ModdedMaterial moddedMat, Level level, BlockPos pos, Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PlateCrafterBlockEntity crafter) {
            crafter.triggerSpin();
        }

        if (!level.isClientSide) {
            int cost = currentSelectedSize.getIngredientCost();
            if (held.getCount() < cost) {
                player.displayClientMessage(
                        Component.literal("§cДля размера " + currentSelectedSize.getName() + " требуется " + cost + " ед. материала!"), true);
                level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.6f, 1.0f);
                return ItemInteractionResult.FAIL;
            }

            // Динамическое определение цвета (сэмплинг центрального пикселя спрайта или каталог)
            int color = DynamicMaterialSampler.sampleItemColor(held);
            held.shrink(cost);

            ItemStack plates = PlateItem.createModded(moddedMat.getId(), moddedMat.getDisplayName(), color, currentSelectedSize, null, currentSelectedSize.getCraftYield());
            if (!player.addItem(plates)) {
                player.drop(plates, false);
            }
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.7f, 1.4f);
            player.displayClientMessage(
                    Component.literal("§bНарезано " + currentSelectedSize.getCraftYield() + " неисследованных пластин (" + currentSelectedSize.getName() + ") из " + moddedMat.getDisplayName() + String.format(" [RGB: #%06X]", (color & 0xFFFFFF))), true);
        }
        return ItemInteractionResult.SUCCESS;
    }

    private VoxelMaterial findMaterialForIngredient(ItemStack stack) {
        String regName = stack.getItem().toString().toLowerCase();
        for (VoxelMaterial mat : VoxelMaterial.values()) {
            if (mat != VoxelMaterial.AIR && mat.getPrimaryIngredient().contains(regName)) {
                return mat;
            }
        }

        // Проверка ванильных материалов
        String desc = stack.getDescriptionId().toLowerCase();
        if (desc.contains("iron_ingot")) return VoxelMaterial.IRON;
        if (desc.contains("gold_ingot")) return VoxelMaterial.GOLD;
        if (desc.contains("copper_ingot")) return VoxelMaterial.COPPER;
        if (desc.contains("diamond")) return VoxelMaterial.DIAMOND;
        if (desc.contains("netherite")) return VoxelMaterial.NETHERITE;
        if (desc.contains("quartz")) return VoxelMaterial.QUARTZ;
        if (desc.contains("redstone")) return VoxelMaterial.REDSTONE;
        if (desc.contains("lapis")) return VoxelMaterial.LAPIS;
        if (desc.contains("emerald")) return VoxelMaterial.EMERALD;
        if (desc.contains("amethyst")) return VoxelMaterial.AMETHYST;
        if (desc.contains("obsidian")) return VoxelMaterial.OBSIDIAN;
        if (desc.contains("leather")) return VoxelMaterial.LEATHER;
        if (desc.contains("bone")) return VoxelMaterial.BONE;
        if (desc.contains("deepslate")) return VoxelMaterial.DEEPSLATE;

        return null;
    }
}
