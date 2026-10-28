package com.voxelstand.block;

import com.mojang.serialization.MapCodec;
import com.voxelstand.api.VoxelConstants;
import com.voxelstand.block.entity.ModBlockEntities;
import com.voxelstand.block.entity.SculptStandBlockEntity;
import com.voxelstand.item.ChiselItem;
import com.voxelstand.item.PlateItem;
import com.voxelstand.item.PlateSize;
import com.voxelstand.stats.ItemStatData;
import com.voxelstand.stats.StatCalculator;
import com.voxelstand.voxel.SymmetryHelper;
import com.voxelstand.voxel.VoxelGrid;
import com.voxelstand.voxel.VoxelMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

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
import java.util.Map;

public class SculptStandBlock extends BaseEntityBlock {
    public static final MapCodec<SculptStandBlock> CODEC = simpleCodec(SculptStandBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public SculptStandBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public SculptStandBlock() {
        this(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5f)
                .sound(SoundType.WOOD)
                .noOcclusion());
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
        BlockPos pAbove = pos.above();
        BlockPos pAboveRight = pAbove.relative(right);

        if (!level.getBlockState(pRight).canBeReplaced(context) ||
            !level.getBlockState(pAbove).canBeReplaced(context) ||
            !level.getBlockState(pAboveRight).canBeReplaced(context)) {
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
            BlockPos pAbove = pos.above();
            BlockPos pAboveRight = pAbove.relative(right);

            setPart(level, pRight, pos);
            setPart(level, pAbove, pos);
            setPart(level, pAboveRight, pos);
        }
    }

    private void setPart(Level level, BlockPos partPos, BlockPos masterPos) {
        level.setBlock(partPos, ModBlocks.STAND_PART.get().defaultBlockState(), 3);
        BlockEntity be = level.getBlockEntity(partPos);
        if (be instanceof StandPartBlockEntity part) {
            part.setMasterPos(masterPos);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            Direction right = facing.getClockWise();
            removePart(level, pos.relative(right));
            removePart(level, pos.above());
            removePart(level, pos.above().relative(right));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private void removePart(Level level, BlockPos partPos) {
        BlockState partState = level.getBlockState(partPos);
        if (partState.is(ModBlocks.STAND_PART.get())) {
            level.setBlock(partPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SculptStandBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        InteractionResult res = handleInteraction(held, state, level, pos, player, hand, hit);
        if (res == InteractionResult.PASS) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return handleInteraction(ItemStack.EMPTY, state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }

    private InteractionResult handleInteraction(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SculptStandBlockEntity stand)) {
            return InteractionResult.PASS;
        }

        // =========================================================================
        // 1. НАСТРОЙКА СТОЙКИ И ЗАВЕРШЕНИЕ: Shift + ПКМ
        // =========================================================================
        if (player.isShiftKeyDown()) {
            if (stand.getMode() != SculptStandBlockEntity.StandMode.EMPTY) {
                // А. Держит долото: переключает симметрию стойки
                if (held.getItem() instanceof ChiselItem) {
                    if (!level.isClientSide) {
                        boolean sym = !stand.isSymmetryMode();
                        stand.setSymmetryMode(sym);
                        player.displayClientMessage(sym ?
                                Component.translatable("message.customsmith.symmetry_enabled") :
                                Component.translatable("message.customsmith.symmetry_disabled"), true);
                        level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.5f, 1.4f);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }

                // Б. Пустая рука: ДВОЙНОЕ ПОДТВЕРЖДЕНИЕ ЗАПЕЧАТЫВАНИЯ ФОРМЫ или СНЯТИЕ ПРИ НАСТРОЙКЕ ПОЗЫ
                if (held.isEmpty()) {
                    if (!level.isClientSide) {
                        // Если предмет уже запечатан (режим настройки позы) — снимается сразу без перековки
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

                        // Подтверждение: второй клик в течение 60 тиков (3 секунды)
                        if (currentTime - lastAttempt <= 60 && currentTime > lastAttempt) {
                            ItemStack finalized = stand.finalizeAndExtractItem();
                            if (!finalized.isEmpty()) {
                                if (!player.addItem(finalized)) {
                                    player.drop(finalized, false);
                                }
                                level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8f, 1.2f);
                                level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.4f);
                                player.displayClientMessage(Component.literal("§a[Кузница] Предмет успешно выкован и запечатан! (Вокселей: " + count + ")"), false);
                            }
                        } else {
                            // Первое нажатие: предупреждение и отсчет 3 секунд
                            stand.setLastFinalizeAttempt(currentTime);
                            player.displayClientMessage(Component.literal("§c[Внимание!] §eВы собираетесь запечатать предмет! Перековка будет невозможна.\n§6Нажмите Shift + ПКМ еще раз в течение 3 секунд для подтверждения."), false);
                            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.8f, 1.2f);
                        }
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }

            // В. Пустая стойка или другой предмет: переключает режим манекена/ориентира
            if (!level.isClientSide) {
                stand.cycleGuideMode();
                String[] modes = {"§eСоломенный манекен", "§bДеревянный каркас и канаты", "§7Скрыт"};
                player.displayClientMessage(Component.literal("§6Настройка стойки: " + modes[stand.getGuideMode()]), true);
                level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.6f, 1.1f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // =========================================================================
        // 2. ИНФОРМАЦИЯ / НАСТРОЙКА ПОЗЫ: Обычный ПКМ пустой рукой
        // =========================================================================
        if (held.isEmpty() && stand.getMode() != SculptStandBlockEntity.StandMode.EMPTY) {
            if (!level.isClientSide) {
                if (stand.isPoseCustomizationOnly()) {
                    // Циклический поворот острия предмета
                    stand.cyclePoseRotation();
                    player.displayClientMessage(Component.literal("§6[Стойка/Поза] Поворот острия: §e" + (int)stand.getCurrentPose().itemRotY + "° " +
                            "§7(Shift + ПКМ пустой рукой — забрать; Долотом — переключить 1/2 руки)"), true);
                    level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.7f, 1.2f);
                } else {
                    int count = stand.getTotalVoxelCount();
                    ItemStatData stats = StatCalculator.calculate(stand.getAllGrids(), stand.getHeldItem());
                    String itemName = stand.getHeldItem().getHoverName().getString();

                    player.displayClientMessage(Component.literal("§6=== [ СТАПЕЛЬ: " + itemName + " ] ==="), false);
                    String inlayText = stats.inlayRatio > 0 ? (" §8(Инкрустация: " + (int) (stats.inlayRatio * 100) + "%)") : "";
                    float qMult = stand.calculateQualityMultiplier();
                    String qText = qMult > 1.0f ? (" §8| §7Качество: §6" + stand.getQualityName() + " (x" + String.format("%.2f", qMult) + ")") : "";
                    player.displayClientMessage(Component.literal("§7Воксели: §e" + count + " / " + VoxelConstants.DEFAULT_VOXEL_LIMIT +
                            " §8| §7Класс: " + stats.weightClass.getFormattedBadge() +
                            " §8| §7Вес: §f" + String.format("%.1f", stats.weight) + inlayText + qText), false);

                    if (stand.hasGhostBlueprint()) {
                        player.displayClientMessage(Component.literal("§d[Голограмма чертежа]: §f" + stand.getGhostBlueprintName() + " §7(прозрачность 0.7)"), false);
                    }

                    if (stand.getHeldItem().getItem() instanceof BowItem || stand.getHeldItem().getItem() instanceof CrossbowItem) {
                        player.displayClientMessage(Component.literal("§7Натяжение: §b" + String.format("%.2fs", stats.drawSpeedTicks / 20.0f) +
                                " (" + (int) stats.drawSpeedTicks + "т) §8| §7Скор. стрелы: §a+" + (int) ((stats.arrowVelocityMult - 1.0f) * 100) + "%" +
                                " §8| §7Бронепробитие: §6" + (int) (stats.armorPenetration * 100) + "%"), false);
                    } else if (stand.getHeldItem().getItem() instanceof ArmorItem) {
                        player.displayClientMessage(Component.literal("§7Защита: §b+" + String.format("%.1f", stats.armor) +
                                " §8| §7Стойкость: §9+" + String.format("%.1f", stats.armorToughness) +
                                " §8| §7Штраф скорости: §c-" + String.format("%.1f%%", Math.abs(stats.movementSpeedPenalty * 100))), false);
                    } else if (stand.getHeldItem().getItem() instanceof ShieldItem) {
                        player.displayClientMessage(Component.literal("§7Сопротивление толчку: §6+" + (int) (stats.knockbackResistance * 100) + "%" +
                                " §8| §7Прочность: §a" + stats.maxDurability), false);
                    } else {
                        player.displayClientMessage(Component.literal("§7Урон: §c+" + String.format("%.1f", stats.damage) +
                                " §8| §7Скор. атаки: §b" + String.format("%.2f", stats.attackSpeed) +
                                " §8| §7Прочность: §a" + stats.maxDurability), false);
                    }

                    if (!stats.enchantments.isEmpty()) {
                        StringBuilder enchSb = new StringBuilder("§dЗачарования: ");
                        for (Map.Entry<ResourceLocation, Integer> entry : stats.enchantments.entrySet()) {
                            enchSb.append("§5✦ ").append(Component.translatable("enchantment." + entry.getKey().getNamespace() + "." + entry.getKey().getPath()).getString()).append(" ").append(entry.getValue()).append(" ");
                        }
                        player.displayClientMessage(Component.literal(enchSb.toString()), false);
                    }
                    player.displayClientMessage(Component.literal("§7(Для завершения ковки нажмите §6Shift + ПКМ дважды§7)"), false);
                    level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.8f);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // =========================================================================
        // 3. УСТАНОВКА НОВОГО ПРЕДМЕТА: ПКМ с оружием или броней
        // =========================================================================
        if (stand.getMode() == SculptStandBlockEntity.StandMode.EMPTY && !held.isEmpty()) {
            if (stand.setItem(held.split(1))) {
                if (!level.isClientSide) {
                    level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.BLOCKS, 0.8f, 1.0f);
                    player.displayClientMessage(Component.literal("§aПредмет установлен на стапель. Авто-каркас сформирован!"), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        // =========================================================================
        // 4. НАСТРОЙКА ПОЗЫ ДОЛОТОМ ИЛИ УСТАНОВКА ПЛАСТИНЫ
        // =========================================================================
        if (stand.getMode() != SculptStandBlockEntity.StandMode.EMPTY) {
            // Если предмет запечатан — долото переключает хват (1/2 руки), а пластины блокируются
            if (stand.isPoseCustomizationOnly()) {
                if (held.getItem() instanceof ChiselItem) {
                    if (!level.isClientSide) {
                        stand.toggleTwoHanded();
                        player.displayClientMessage(Component.literal(stand.getCurrentPose().twoHanded ?
                                "§a[Стойка] Переключено на ДВУРУЧНЫЙ хват!" :
                                "§e[Стойка] Переключено на ОДНОРУЧНЫЙ хват."), true);
                        level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.5f, 1.5f);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
                if (held.getItem() instanceof PlateItem) {
                    if (!level.isClientSide) {
                        player.displayClientMessage(Component.literal("§c[Кузница] Форма предмета уже запечатана! Доступна только смена позы и хвата."), true);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }

            if (held.getItem() instanceof PlateItem) {
                if (!level.isClientSide) {
                    if (!PlateItem.hasQuality(held)) {
                        player.displayClientMessage(Component.literal("§c[Кузница] Пластина не исследована! Возьмите подзорную трубу в левую руку и зажмите ПКМ для оценки качества."), true);
                        level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.7f, 1.0f);
                        return InteractionResult.CONSUME;
                    }

                    VoxelMaterial mat = PlateItem.getMaterial(held);
                    PlateSize size = PlateItem.getSize(held);
                    boolean symmetry = stand.isSymmetryMode();

                    if (symmetry && held.getCount() < 2) {
                        player.displayClientMessage(Component.translatable("message.customsmith.not_enough_plates"), true);
                        return InteractionResult.CONSUME;
                    }

                    // Выбираем сетку для редактирования (для оружия "weapon", для брони основная деталь)
                    String targetPart = stand.getMode() == SculptStandBlockEntity.StandMode.WEAPON ? "weapon" :
                            (stand.getMode() == SculptStandBlockEntity.StandMode.HELMET ? "head" :
                            (stand.getMode() == SculptStandBlockEntity.StandMode.CHESTPLATE ? "body" :
                            (stand.getMode() == SculptStandBlockEntity.StandMode.LEGGINGS ? "pelvis" : "left_boot")));

                    VoxelGrid targetGrid = stand.getGrid(targetPart);

                    int ox = 16 - (size.getWidth() / 2);
                    int oy = 10;
                    int oz = 15;
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
        }

        return InteractionResult.PASS;
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        // ЛКМ резцом: скалывание 1x1x1 вокселя (с учетом симметрии)
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof ChiselItem) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SculptStandBlockEntity stand && stand.getMode() != SculptStandBlockEntity.StandMode.EMPTY) {
                if (!level.isClientSide) {
                    if (stand.isPoseCustomizationOnly()) {
                        player.displayClientMessage(Component.literal("§c[Кузница] Форма предмета запечатана! Изменение вокселей невозможно."), true);
                        return;
                    }

                    String targetPart = stand.getMode() == SculptStandBlockEntity.StandMode.WEAPON ? "weapon" : "head";
                    VoxelGrid g = stand.getGrid(targetPart);
                    // Скалывание
                    if (stand.isSymmetryMode()) {
                        SymmetryHelper.removeSymmetricalX(g, 16, 20, 15);
                    } else {
                        g.remove(16, 20, 15);
                    }
                    stand.setChanged();
                    level.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 0.7f, 1.8f);
                }
            }
        }
        super.attack(state, level, pos, player);
    }
}
