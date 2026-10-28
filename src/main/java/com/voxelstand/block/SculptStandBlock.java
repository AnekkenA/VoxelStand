package com.voxelstand.block;

import com.mojang.serialization.MapCodec;
import com.voxelstand.api.VoxelConstants;
import com.voxelstand.block.entity.ModBlockEntities;
import com.voxelstand.block.entity.SculptStandBlockEntity;
import com.voxelstand.block.entity.StandPartBlockEntity;
import com.voxelstand.item.ChiselItem;
import com.voxelstand.item.PlateItem;
import com.voxelstand.item.PlateSize;
import com.voxelstand.voxel.SymmetryHelper;
import com.voxelstand.voxel.VoxelGrid;
import com.voxelstand.voxel.VoxelMaterial;
import com.voxelstand.voxel.VoxelRaycastHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Универсальный стапель кузницы (Minecraft 1.21.1 NeoForge).
 * Двухблочная горизонтальная мультиблок-структура (2x1x1) без верхних фантомных блоков.
 * Пропускает свет без затенения соседних блоков.
 */
public class SculptStandBlock extends BaseEntityBlock {
    public static final MapCodec<SculptStandBlock> CODEC = simpleCodec(SculptStandBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<SculptStandBlockEntity.StandMode> MODE =
            EnumProperty.create("mode", SculptStandBlockEntity.StandMode.class);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public SculptStandBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(MODE, SculptStandBlockEntity.StandMode.EMPTY));
    }

    public SculptStandBlock() {
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
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, MODE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        Direction right = facing.getClockWise();
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();

        BlockPos pRight = pos.relative(right);

        // Стапель занимает ровно 2 горизонтальных блока (pos и pos.relative(right))
        if (!level.getBlockState(pRight).canBeReplaced(context)) {
            return null;
        }

        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(MODE, SculptStandBlockEntity.StandMode.EMPTY);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            Direction right = facing.getClockWise();
            BlockPos partPos = pos.relative(right);

            level.setBlock(partPos, ModBlocks.STAND_PART.get().defaultBlockState(), 3);
            BlockEntity be = level.getBlockEntity(partPos);
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

            if (level.getBlockState(pRight).is(ModBlocks.STAND_PART.get())) {
                level.destroyBlock(pRight, false);
            }

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SculptStandBlockEntity stand) {
                ItemStack held = stand.getHeldItem();
                if (!held.isEmpty()) {
                    net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), held);
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                Direction facing = state.hasProperty(FACING) ? state.getValue(FACING) : Direction.NORTH;
                BlockPos pRight = pos.relative(facing.getClockWise());

                BlockState partState = level.getBlockState(pRight);
                if (partState.is(ModBlocks.STAND_PART.get())) {
                    level.removeBlock(pRight, false);
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
        return new SculptStandBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SculptStandBlockEntity stand)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        InteractionResult result = handleInteraction(stand, held, state, level, pos, player, hand, hit);
        if (result == InteractionResult.CONSUME) {
            return ItemInteractionResult.CONSUME;
        } else if (result.consumesAction()) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SculptStandBlockEntity stand)) {
            return InteractionResult.PASS;
        }
        return handleInteraction(stand, ItemStack.EMPTY, state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }

    private InteractionResult handleInteraction(SculptStandBlockEntity stand, ItemStack held, BlockState state,
                                                Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

        // =========================================================================
        // 1. ИЗВЛЕЧЕНИЕ ПРЕДМЕТА ИЛИ ПЕРЕКЛЮЧЕНИЕ МАНЕКЕНА: Shift + ПКМ пустой рукой
        // =========================================================================
        if (player.isShiftKeyDown() && held.isEmpty()) {
            if (stand.getMode() != SculptStandBlockEntity.StandMode.EMPTY) {
                if (!level.isClientSide) {
                    if (stand.isPoseCustomizationOnly()) {
                        ItemStack updated = stand.finalizeAndExtractItem();
                        if (!player.addItem(updated)) {
                            player.drop(updated, false);
                        }
                        level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
                        player.displayClientMessage(Component.literal("§a[Кузница] Предмет снят со стапеля с обновленной позой!"), true);
                        return InteractionResult.sidedSuccess(level.isClientSide);
                    }

                    int count = stand.getTotalVoxelCount();
                    if (count > VoxelConstants.DEFAULT_VOXEL_LIMIT) {
                        player.displayClientMessage(Component.translatable("message.customsmith.voxel_limit_exceeded", count, VoxelConstants.DEFAULT_VOXEL_LIMIT), true);
                        level.playSound(null, pos, SoundEvents.ANVIL_HIT, SoundSource.BLOCKS, 0.7f, 0.6f);
                        return InteractionResult.CONSUME;
                    }

                    long currentTime = level.getGameTime();
                    long lastAttempt = stand.getLastFinalizeAttempt();

                    if (currentTime - lastAttempt <= 60 && currentTime > lastAttempt) {
                        ItemStack finalized = stand.finalizeAndExtractItem();
                        if (!finalized.isEmpty()) {
                            if (!player.addItem(finalized)) {
                                player.drop(finalized, false);
                            }
                            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8f, 1.2f);
                            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.4f);
                            player.displayClientMessage(Component.literal("§a[Кузница] Предмет успешно выкован и запечатан! (Вокселей: " + count + ")"), true);
                        }
                    } else {
                        stand.setLastFinalizeAttempt(currentTime);
                        player.displayClientMessage(Component.literal("§eНажмите Shift + ПКМ еще раз в течение 3 секунд для запечатывания!"), true);
                        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.8f, 1.2f);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            // Пустая стойка: переключает режим манекена
            if (!level.isClientSide) {
                stand.cycleGuideMode();
                String[] modes = {"§eСоломенный манекен", "§bДеревянный каркас", "§7Скрыт"};
                player.displayClientMessage(Component.literal("§6Настройка стойки: " + modes[stand.getGuideMode()]), true);
                level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.6f, 1.1f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // =========================================================================
        // 2. ИНФОРМАЦИЯ / СМЕНА РЕЖИМА / НАСТРОЙКА ПОЗЫ: Обычный ПКМ пустой рукой
        // =========================================================================
        if (held.isEmpty() && stand.getMode() != SculptStandBlockEntity.StandMode.EMPTY) {
            if (!level.isClientSide) {
                if (stand.isPoseCustomizationOnly()) {
                    stand.cyclePoseRotation();
                    level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.7f, 1.2f);
                } else {
                    stand.toggleCosmeticMode();
                    if (stand.isCosmeticMode()) {
                        player.displayClientMessage(Component.literal("§b[Режим: Косметика] Внешний вид изменится, а все характеристики и чары останутся оригинальными!"), true);
                    } else {
                        player.displayClientMessage(Component.literal("§6[Режим: Ковка] Включен полный расчет характеристик, веса и баланса!"), true);
                    }
                    level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.7f, 1.5f);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // =========================================================================
        // 3. УСТАНОВКА НОВОГО ПРЕДМЕТА: ПКМ (с авто-каркасом) или Shift + ПКМ (с нуля)
        // =========================================================================
        if (stand.getMode() == SculptStandBlockEntity.StandMode.EMPTY && !held.isEmpty()) {
            if (!(held.getItem() instanceof ChiselItem) && !(held.getItem() instanceof PlateItem)) {
                ItemStack candidate = held.copyWithCount(1);
                if (stand.canAcceptItem(candidate)) {
                    boolean startFromScratch = player.isShiftKeyDown();
                    if (stand.setItem(candidate, startFromScratch)) {
                        held.shrink(1);
                        if (!level.isClientSide) {
                            level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.BLOCKS, 0.8f, 1.0f);
                            if (startFromScratch) {
                                player.displayClientMessage(Component.literal("§e[Кузница] Предмет установлен! Сетка пуста — моделирование с нуля."), true);
                            } else {
                                player.displayClientMessage(Component.literal("§a[Кузница] Предмет установлен! Каркас сформирован по текстуре предмета."), true);
                            }
                        }
                        return InteractionResult.sidedSuccess(level.isClientSide);
                    }
                }
            }
        }

        // =========================================================================
        // 4. ДОЛОТО: Скалывание вокселя (ПКМ), смена тона (Shift + ПКМ), или симметрия
        // =========================================================================
        if (stand.getMode() != SculptStandBlockEntity.StandMode.EMPTY && held.getItem() instanceof ChiselItem) {
            if (!level.isClientSide) {
                if (stand.isPoseCustomizationOnly()) {
                    stand.toggleTwoHanded();
                    player.displayClientMessage(Component.literal(stand.getCurrentPose().twoHanded ?
                            "§a[Стойка] Переключено на ДВУРУЧНЫЙ хват!" :
                            "§e[Стойка] Переключено на ОДНОРУЧНЫЙ хват."), true);
                    level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.5f, 1.5f);
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }

                VoxelRaycastHelper.RaycastHit rayHit = VoxelRaycastHelper.raycast(player, stand, pos);
                if (rayHit != null) {
                    VoxelGrid g = stand.getGrid(rayHit.partName);
                    if (g != null && g.get(rayHit.vx, rayHit.vy, rayHit.vz) != 0) {
                        byte oldVal = g.get(rayHit.vx, rayHit.vy, rayHit.vz);

                        if (player.isShiftKeyDown()) {
                            // Смена тона вокселя: Базовый -> Светлый -> Тёмный -> Глубокий
                            int baseMat = oldVal & 0x1F;
                            int curTone = ((oldVal & 0xFF) >> 5) & 3;
                            int nextTone = (curTone + 1) % 4;
                            byte newVal = (byte) (baseMat | (nextTone << 5));

                            if (stand.isSymmetryMode()) {
                                int symX = 31 - rayHit.vx;
                                g.set(rayHit.vx, rayHit.vy, rayHit.vz, newVal);
                                if (g.get(symX, rayHit.vy, rayHit.vz) != 0) {
                                    g.set(symX, rayHit.vy, rayHit.vz, newVal);
                                }
                            } else {
                                g.set(rayHit.vx, rayHit.vy, rayHit.vz, newVal);
                            }
                            stand.setChanged();

                            String[] toneNames = {"§fБазовый", "§bСветлый (+18%)", "§7Тёмный (тень)", "§8Глубокий (рельеф)"};
                            player.displayClientMessage(Component.literal("§e[Долото] Тон вокселя: " + toneNames[nextTone]), true);
                            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 0.8f, 1.4f);
                            return InteractionResult.sidedSuccess(level.isClientSide);
                        } else {
                            // Скалывание вокселя по обычному ПКМ
                            if (stand.isSymmetryMode()) {
                                SymmetryHelper.removeSymmetricalX(g, rayHit.vx, rayHit.vy, rayHit.vz);
                            } else {
                                g.remove(rayHit.vx, rayHit.vy, rayHit.vz);
                            }
                            stand.setChanged();
                            level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.7f, 1.8f);
                            return InteractionResult.sidedSuccess(level.isClientSide);
                        }
                    }
                }

                // Если луч не попал в воксел: переключаем симметрию
                boolean sym = !stand.isSymmetryMode();
                stand.setSymmetryMode(sym);
                player.displayClientMessage(sym ?
                        Component.translatable("message.customsmith.symmetry_enabled") :
                        Component.translatable("message.customsmith.symmetry_disabled"), true);
                level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.5f, 1.4f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // =========================================================================
        // 5. УСТАНОВКА ПЛАСТИНЫ: ПКМ с пластиной (любое качество, включая креатив)
        // =========================================================================
        if (stand.getMode() != SculptStandBlockEntity.StandMode.EMPTY && held.getItem() instanceof PlateItem) {
            if (stand.isPoseCustomizationOnly()) {
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.literal("§c[Кузница] Форма предмета уже запечатана! Доступна только смена позы и хвата."), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            if (!level.isClientSide) {
                VoxelMaterial mat = PlateItem.getMaterial(held);
                PlateSize size = PlateItem.getSize(held);
                boolean symmetry = stand.isSymmetryMode();

                if (symmetry && held.getCount() < 2) {
                    player.displayClientMessage(Component.translatable("message.customsmith.not_enough_plates"), true);
                    return InteractionResult.CONSUME;
                }

                VoxelRaycastHelper.RaycastHit rayHit = VoxelRaycastHelper.raycast(player, stand, pos);
                int placeX = rayHit != null ? rayHit.placeX : 16;
                int placeY = rayHit != null ? rayHit.placeY : 10;
                int placeZ = rayHit != null ? rayHit.placeZ : 15;
                String targetPart = rayHit != null ? rayHit.partName : "weapon";

                VoxelGrid targetGrid = stand.getGrid(targetPart);
                if (targetGrid == null) targetGrid = stand.getGrid("weapon");
                if (targetGrid == null && !stand.getAllGrids().isEmpty()) {
                    targetGrid = stand.getAllGrids().values().iterator().next();
                }
                if (targetGrid == null) return InteractionResult.PASS;

                int ox = Math.max(0, Math.min(32 - size.getWidth(), placeX - (size.getWidth() / 2)));
                int oy = Math.max(0, Math.min(32 - size.getHeight(), placeY));
                int oz = Math.max(0, Math.min(31, placeZ));
                int[] stepU = {1, 0, 0};
                int[] stepV = {0, 1, 0};

                int placed;
                if (symmetry) {
                    placed = SymmetryHelper.placePlateSymmetricalX(targetGrid, ox, oy, oz, stepU, stepV, size.getWidth(), size.getHeight(), mat.getId());
                } else {
                    placed = targetGrid.placePlate(ox, oy, oz, stepU, stepV, size.getWidth(), size.getHeight(), mat.getId());
                }

                if (placed > 0) {
                    int consumed = symmetry ? 2 : 1;
                    com.voxelstand.item.PlateQuality quality = PlateItem.getQuality(held);
                    if (quality == null) quality = com.voxelstand.item.PlateQuality.NORMAL;
                    held.shrink(consumed);
                    stand.recordPlatePlacement(quality, consumed);
                    stand.setChanged();
                    level.playSound(null, pos, SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 0.7f, 1.3f);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        // ЛКМ долотом: скалывание целевого вокселя по 3D-рейкасту
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof ChiselItem) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SculptStandBlockEntity stand && stand.getMode() != SculptStandBlockEntity.StandMode.EMPTY) {
                if (!level.isClientSide) {
                    if (stand.isPoseCustomizationOnly()) {
                        player.displayClientMessage(Component.literal("§c[Кузница] Форма предмета запечатана! Изменение вокселей невозможно."), true);
                        return;
                    }

                    if (player.isShiftKeyDown()) {
                        // Shift + ЛКМ долотом: полная очистка сетки для моделирования с нуля!
                        stand.clearGrids();
                        player.displayClientMessage(Component.literal("§c[Сетка очищена] Все воксели сброшены. Моделирование с нуля!"), true);
                        level.playSound(null, pos, SoundEvents.ANVIL_BREAK, SoundSource.BLOCKS, 0.7f, 1.5f);
                        return;
                    }

                    VoxelRaycastHelper.RaycastHit hit = VoxelRaycastHelper.raycast(player, stand, pos);
                    if (hit != null) {
                        VoxelGrid g = stand.getGrid(hit.partName);
                        if (g != null && g.get(hit.vx, hit.vy, hit.vz) != 0) {
                            if (stand.isSymmetryMode()) {
                                SymmetryHelper.removeSymmetricalX(g, hit.vx, hit.vy, hit.vz);
                            } else {
                                g.remove(hit.vx, hit.vy, hit.vz);
                            }
                            stand.setChanged();
                            level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.7f, 1.8f);
                            return;
                        }
                    }
                }
            }
        }
        super.attack(state, level, pos, player);
    }
}
