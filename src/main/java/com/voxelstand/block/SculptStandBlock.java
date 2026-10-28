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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import java.util.Map;

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
                .noOcclusion());
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
        BlockPos pAbove = pos.above();
        BlockPos pAboveRight = pAbove.relative(right);

        if (!level.getBlockState(pRight).canBeReplaced(context) ||
            !level.getBlockState(pAbove).canBeReplaced(context) ||
            !level.getBlockState(pAboveRight).canBeReplaced(context)) {
            return null;
        }

        return this.defaultBlockState().setValue(FACING, facing).setValue(MODE, SculptStandBlockEntity.StandMode.EMPTY);
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
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                Direction facing = state.getValue(FACING);
                Direction right = facing.getClockWise();
                removePart(level, pos.relative(right));
                removePart(level, pos.above());
                removePart(level, pos.above().relative(right));
            }
            super.onRemove(state, level, pos, newState, isMoving);
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
            level.removeBlock(partPos, false);
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
        // 1. РќРђРЎРўР РћР™РљРђ РЎРўРћР™РљР Р Р—РђР’Р•Р РЁР•РќРР•: Shift + РџРљРњ
        // =========================================================================
        if (player.isShiftKeyDown()) {
            if (stand.getMode() != SculptStandBlockEntity.StandMode.EMPTY) {
                // Рђ. Р”РµСЂР¶РёС‚ РґРѕР»РѕС‚Рѕ: РїРµСЂРµРєР»СЋС‡Р°РµС‚ СЃРёРјРјРµС‚СЂРёСЋ СЃС‚РѕР№РєРё
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

                // Р‘. РџСѓСЃС‚Р°СЏ СЂСѓРєР°: Р”Р’РћР™РќРћР• РџРћР”РўР’Р•Р Р–Р”Р•РќРР• Р—РђРџР•Р§РђРўР«Р’РђРќРРЇ Р¤РћР РњР« РёР»Рё РЎРќРЇРўРР• РџР Р РќРђРЎРўР РћР™РљР• РџРћР—Р«
                if (held.isEmpty()) {
                    if (!level.isClientSide) {
                        // Р•СЃР»Рё РїСЂРµРґРјРµС‚ СѓР¶Рµ Р·Р°РїРµС‡Р°С‚Р°РЅ (СЂРµР¶РёРј РЅР°СЃС‚СЂРѕР№РєРё РїРѕР·С‹) вЂ” СЃРЅРёРјР°РµС‚СЃСЏ СЃСЂР°Р·Сѓ Р±РµР· РїРµСЂРµРєРѕРІРєРё
                        if (stand.isPoseCustomizationOnly()) {
                            ItemStack updated = stand.finalizeAndExtractItem();
                            if (!player.addItem(updated)) {
                                player.drop(updated, false);
                            }
                            level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
                            player.displayClientMessage(Component.literal("В§a[РљСѓР·РЅРёС†Р°] РџСЂРµРґРјРµС‚ СЃРЅСЏС‚ СЃРѕ СЃС‚Р°РїРµР»СЏ СЃ РѕР±РЅРѕРІР»РµРЅРЅРѕР№ РїРѕР·РѕР№!"), true);
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

                        // РџРѕРґС‚РІРµСЂР¶РґРµРЅРёРµ: РІС‚РѕСЂРѕР№ РєР»РёРє РІ С‚РµС‡РµРЅРёРµ 60 С‚РёРєРѕРІ (3 СЃРµРєСѓРЅРґС‹)
                        if (currentTime - lastAttempt <= 60 && currentTime > lastAttempt) {
                            ItemStack finalized = stand.finalizeAndExtractItem();
                            if (!finalized.isEmpty()) {
                                if (!player.addItem(finalized)) {
                                    player.drop(finalized, false);
                                }
                                level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8f, 1.2f);
                                level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.4f);
                                player.displayClientMessage(Component.literal("В§a[РљСѓР·РЅРёС†Р°] РџСЂРµРґРјРµС‚ СѓСЃРїРµС€РЅРѕ РІС‹РєРѕРІР°РЅ Рё Р·Р°РїРµС‡Р°С‚Р°РЅ! (Р’РѕРєСЃРµР»РµР№: " + count + ")"), false);
                            }
                        } else {
                            // РџРµСЂРІРѕРµ РЅР°Р¶Р°С‚РёРµ: РїСЂРµРґСѓРїСЂРµР¶РґРµРЅРёРµ Рё РѕС‚СЃС‡РµС‚ 3 СЃРµРєСѓРЅРґ
                            stand.setLastFinalizeAttempt(currentTime);
                            player.displayClientMessage(Component.literal("В§c[Р’РЅРёРјР°РЅРёРµ!] В§eР’С‹ СЃРѕР±РёСЂР°РµС‚РµСЃСЊ Р·Р°РїРµС‡Р°С‚Р°С‚СЊ РїСЂРµРґРјРµС‚! РџРµСЂРµРєРѕРІРєР° Р±СѓРґРµС‚ РЅРµРІРѕР·РјРѕР¶РЅР°.\nВ§6РќР°Р¶РјРёС‚Рµ Shift + РџРљРњ РµС‰Рµ СЂР°Р· РІ С‚РµС‡РµРЅРёРµ 3 СЃРµРєСѓРЅРґ РґР»СЏ РїРѕРґС‚РІРµСЂР¶РґРµРЅРёСЏ."), false);
                            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.8f, 1.2f);
                        }
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }

            // Р’. РџСѓСЃС‚Р°СЏ СЃС‚РѕР№РєР° РёР»Рё РґСЂСѓРіРѕР№ РїСЂРµРґРјРµС‚: РїРµСЂРµРєР»СЋС‡Р°РµС‚ СЂРµР¶РёРј РјР°РЅРµРєРµРЅР°/РѕСЂРёРµРЅС‚РёСЂР°
            if (!level.isClientSide) {
                stand.cycleGuideMode();
                String[] modes = {"В§eРЎРѕР»РѕРјРµРЅРЅС‹Р№ РјР°РЅРµРєРµРЅ", "В§bР”РµСЂРµРІСЏРЅРЅС‹Р№ РєР°СЂРєР°СЃ Рё РєР°РЅР°С‚С‹", "В§7РЎРєСЂС‹С‚"};
                player.displayClientMessage(Component.literal("В§6РќР°СЃС‚СЂРѕР№РєР° СЃС‚РѕР№РєРё: " + modes[stand.getGuideMode()]), true);
                level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.6f, 1.1f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // =========================================================================
        // 2. РРќР¤РћР РњРђР¦РРЇ / РќРђРЎРўР РћР™РљРђ РџРћР—Р«: РћР±С‹С‡РЅС‹Р№ РџРљРњ РїСѓСЃС‚РѕР№ СЂСѓРєРѕР№
        // =========================================================================
        if (held.isEmpty() && stand.getMode() != SculptStandBlockEntity.StandMode.EMPTY) {
            if (!level.isClientSide) {
                if (stand.isPoseCustomizationOnly()) {
                    // Р¦РёРєР»РёС‡РµСЃРєРёР№ РїРѕРІРѕСЂРѕС‚ РѕСЃС‚СЂРёСЏ РїСЂРµРґРјРµС‚Р°
                    stand.cyclePoseRotation();
                    player.displayClientMessage(Component.literal("В§6[РЎС‚РѕР№РєР°/РџРѕР·Р°] РџРѕРІРѕСЂРѕС‚ РѕСЃС‚СЂРёСЏ: В§e" + (int)stand.getCurrentPose().itemRotY + "В° " +
                            "В§7(Shift + РџРљРњ РїСѓСЃС‚РѕР№ СЂСѓРєРѕР№ вЂ” Р·Р°Р±СЂР°С‚СЊ; Р”РѕР»РѕС‚РѕРј вЂ” РїРµСЂРµРєР»СЋС‡РёС‚СЊ 1/2 СЂСѓРєРё)"), true);
                    level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.7f, 1.2f);
                } else {
                    int count = stand.getTotalVoxelCount();
                    ItemStatData stats = StatCalculator.calculate(stand.getAllGrids(), stand.getHeldItem());
                    String itemName = stand.getHeldItem().getHoverName().getString();

                    player.displayClientMessage(Component.literal("В§6=== [ РЎРўРђРџР•Р›Р¬: " + itemName + " ] ==="), false);
                    String inlayText = stats.inlayRatio > 0 ? (" В§8(РРЅРєСЂСѓСЃС‚Р°С†РёСЏ: " + (int) (stats.inlayRatio * 100) + "%)") : "";
                    float qMult = stand.calculateQualityMultiplier();
                    String qText = qMult > 1.0f ? (" В§8| В§7РљР°С‡РµСЃС‚РІРѕ: В§6" + stand.getQualityName() + " (x" + String.format("%.2f", qMult) + ")") : "";
                    player.displayClientMessage(Component.literal("В§7Р’РѕРєСЃРµР»Рё: В§e" + count + " / " + VoxelConstants.DEFAULT_VOXEL_LIMIT +
                            " В§8| В§7РљР»Р°СЃСЃ: " + stats.weightClass.getFormattedBadge() +
                            " В§8| В§7Р’РµСЃ: В§f" + String.format("%.1f", stats.weight) + inlayText + qText), false);

                    if (stand.hasGhostBlueprint()) {
                        player.displayClientMessage(Component.literal("В§d[Р“РѕР»РѕРіСЂР°РјРјР° С‡РµСЂС‚РµР¶Р°]: В§f" + stand.getGhostBlueprintName() + " В§7(РїСЂРѕР·СЂР°С‡РЅРѕСЃС‚СЊ 0.7)"), false);
                    }

                    if (stand.getHeldItem().getItem() instanceof BowItem || stand.getHeldItem().getItem() instanceof CrossbowItem) {
                        player.displayClientMessage(Component.literal("В§7РќР°С‚СЏР¶РµРЅРёРµ: В§b" + String.format("%.2fs", stats.drawSpeedTicks / 20.0f) +
                                " (" + (int) stats.drawSpeedTicks + "С‚) В§8| В§7РЎРєРѕСЂ. СЃС‚СЂРµР»С‹: В§a+" + (int) ((stats.arrowVelocityMult - 1.0f) * 100) + "%" +
                                " В§8| В§7Р‘СЂРѕРЅРµРїСЂРѕР±РёС‚РёРµ: В§6" + (int) (stats.armorPenetration * 100) + "%"), false);
                    } else if (stand.getHeldItem().getItem() instanceof ArmorItem) {
                        player.displayClientMessage(Component.literal("В§7Р—Р°С‰РёС‚Р°: В§b+" + String.format("%.1f", stats.armor) +
                                " В§8| В§7РЎС‚РѕР№РєРѕСЃС‚СЊ: В§9+" + String.format("%.1f", stats.armorToughness) +
                                " В§8| В§7РЁС‚СЂР°С„ СЃРєРѕСЂРѕСЃС‚Рё: В§c-" + String.format("%.1f%%", Math.abs(stats.movementSpeedPenalty * 100))), false);
                    } else if (stand.getHeldItem().getItem() instanceof ShieldItem) {
                        player.displayClientMessage(Component.literal("В§7РЎРѕРїСЂРѕС‚РёРІР»РµРЅРёРµ С‚РѕР»С‡РєСѓ: В§6+" + (int) (stats.knockbackResistance * 100) + "%" +
                                " В§8| В§7РџСЂРѕС‡РЅРѕСЃС‚СЊ: В§a" + stats.maxDurability), false);
                    } else {
                        player.displayClientMessage(Component.literal("В§7РЈСЂРѕРЅ: В§c+" + String.format("%.1f", stats.damage) +
                                " В§8| В§7РЎРєРѕСЂ. Р°С‚Р°РєРё: В§b" + String.format("%.2f", stats.attackSpeed) +
                                " В§8| В§7РџСЂРѕС‡РЅРѕСЃС‚СЊ: В§a" + stats.maxDurability), false);
                    }

                    if (!stats.enchantments.isEmpty()) {
                        StringBuilder enchSb = new StringBuilder("В§dР—Р°С‡Р°СЂРѕРІР°РЅРёСЏ: ");
                        for (Map.Entry<ResourceLocation, Integer> entry : stats.enchantments.entrySet()) {
                            enchSb.append("В§5вњ¦ ").append(Component.translatable("enchantment." + entry.getKey().getNamespace() + "." + entry.getKey().getPath()).getString()).append(" ").append(entry.getValue()).append(" ");
                        }
                        player.displayClientMessage(Component.literal(enchSb.toString()), false);
                    }
                    player.displayClientMessage(Component.literal("В§7(Р”Р»СЏ Р·Р°РІРµСЂС€РµРЅРёСЏ РєРѕРІРєРё РЅР°Р¶РјРёС‚Рµ В§6Shift + РџРљРњ РґРІР°Р¶РґС‹В§7)"), false);
                    level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.8f);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // =========================================================================
        // 3. РЈРЎРўРђРќРћР’РљРђ РќРћР’РћР“Рћ РџР Р•Р”РњР•РўРђ: РџРљРњ СЃ РѕСЂСѓР¶РёРµРј РёР»Рё Р±СЂРѕРЅРµР№
        // =========================================================================
        if (stand.getMode() == SculptStandBlockEntity.StandMode.EMPTY && !held.isEmpty()) {
            if (!(held.getItem() instanceof ChiselItem) && !(held.getItem() instanceof PlateItem)) {
                ItemStack candidate = held.copyWithCount(1);
                if (stand.canAcceptItem(candidate)) {
                    if (stand.setItem(candidate)) {
                        held.shrink(1);
                        if (!level.isClientSide) {
                            level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.BLOCKS, 0.8f, 1.0f);
                            player.displayClientMessage(Component.literal("§aПредмет установлен на стапель. Авто-каркас сформирован!"), true);
                        }
                        return InteractionResult.sidedSuccess(level.isClientSide);
                    }
                }
            }
        }

        // =========================================================================
        // 4. РќРђРЎРўР РћР™РљРђ РџРћР—Р« Р”РћР›РћРўРћРњ РР›Р РЈРЎРўРђРќРћР’РљРђ РџР›РђРЎРўРРќР«
        // =========================================================================
        if (stand.getMode() != SculptStandBlockEntity.StandMode.EMPTY) {
            if (held.getItem() instanceof ChiselItem) {
                if (!level.isClientSide) {
                    if (stand.isPoseCustomizationOnly()) {
                        stand.toggleTwoHanded();
                        player.displayClientMessage(Component.literal(stand.getCurrentPose().twoHanded ?
                                "§a[Стойка] Переключено на ДВУРУЧНЫЙ хват!" :
                                "§e[Стойка] Переключено на ОДНОРУЧНЫЙ хват."), true);
                        level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.5f, 1.5f);
                    } else {
                        boolean sym = !stand.isSymmetryMode();
                        stand.setSymmetryMode(sym);
                        player.displayClientMessage(sym ?
                                Component.translatable("message.customsmith.symmetry_enabled") :
                                Component.translatable("message.customsmith.symmetry_disabled"), true);
                        level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.5f, 1.4f);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            if (stand.isPoseCustomizationOnly()) {
                if (held.getItem() instanceof PlateItem) {
                    if (!level.isClientSide) {
                        player.displayClientMessage(Component.literal("В§c[РљСѓР·РЅРёС†Р°] Р¤РѕСЂРјР° РїСЂРµРґРјРµС‚Р° СѓР¶Рµ Р·Р°РїРµС‡Р°С‚Р°РЅР°! Р”РѕСЃС‚СѓРїРЅР° С‚РѕР»СЊРєРѕ СЃРјРµРЅР° РїРѕР·С‹ Рё С…РІР°С‚Р°."), true);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }

            if (held.getItem() instanceof PlateItem) {
                if (!level.isClientSide) {
                    if (!PlateItem.hasQuality(held)) {
                        player.displayClientMessage(Component.literal("В§c[РљСѓР·РЅРёС†Р°] РџР»Р°СЃС‚РёРЅР° РЅРµ РёСЃСЃР»РµРґРѕРІР°РЅР°! Р’РѕР·СЊРјРёС‚Рµ РїРѕРґР·РѕСЂРЅСѓСЋ С‚СЂСѓР±Сѓ РІ Р»РµРІСѓСЋ СЂСѓРєСѓ Рё Р·Р°Р¶РјРёС‚Рµ РџРљРњ РґР»СЏ РѕС†РµРЅРєРё РєР°С‡РµСЃС‚РІР°."), true);
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

                    // Р’С‹Р±РёСЂР°РµРј СЃРµС‚РєСѓ РґР»СЏ СЂРµРґР°РєС‚РёСЂРѕРІР°РЅРёСЏ (РґР»СЏ РѕСЂСѓР¶РёСЏ "weapon", РґР»СЏ Р±СЂРѕРЅРё РѕСЃРЅРѕРІРЅР°СЏ РґРµС‚Р°Р»СЊ)
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
        // Р›РљРњ СЂРµР·С†РѕРј: СЃРєР°Р»С‹РІР°РЅРёРµ 1x1x1 РІРѕРєСЃРµР»СЏ (СЃ СѓС‡РµС‚РѕРј СЃРёРјРјРµС‚СЂРёРё)
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof ChiselItem) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SculptStandBlockEntity stand && stand.getMode() != SculptStandBlockEntity.StandMode.EMPTY) {
                if (!level.isClientSide) {
                    if (stand.isPoseCustomizationOnly()) {
                        player.displayClientMessage(Component.literal("В§c[РљСѓР·РЅРёС†Р°] Р¤РѕСЂРјР° РїСЂРµРґРјРµС‚Р° Р·Р°РїРµС‡Р°С‚Р°РЅР°! РР·РјРµРЅРµРЅРёРµ РІРѕРєСЃРµР»РµР№ РЅРµРІРѕР·РјРѕР¶РЅРѕ."), true);
                        return;
                    }

                    String targetPart = stand.getMode() == SculptStandBlockEntity.StandMode.WEAPON ? "weapon" : "head";
                    VoxelGrid g = stand.getGrid(targetPart);
                    // РЎРєР°Р»С‹РІР°РЅРёРµ
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
