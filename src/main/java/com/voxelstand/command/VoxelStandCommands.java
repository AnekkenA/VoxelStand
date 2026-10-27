package com.voxelstand.command;

import com.voxelstand.block.entity.SculptStandBlockEntity;
import com.voxelstand.blueprint.BlueprintManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/**
 * Игровые команды мода voxelstand:
 * /customsmith save <name>    - сохранить воксельную модель со стапеля в чертеж (config/customsmith/blueprints/<name>.json)
 * /customsmith ghost <name>   - спроецировать чертеж на стапель в виде полупрозрачной голограммы (alpha = 0.7) для выживания
 * /customsmith load <name>    - мгновенно выстроить модель на стапеле (креатив/тестирование)
 * /customsmith give <name>    - выдать готовый выкованный предмет из чертежа в инвентарь игрока (креатив)
 * /customsmith clear          - очистить голограмму-призрак со стапеля
 * /customsmith list           - список всех сохраненных чертежей
 */
public class VoxelStandCommands {

    private static final SuggestionProvider<CommandSourceStack> BLUEPRINT_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(BlueprintManager.listBlueprints(), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var baseCommand = Commands.literal("voxelstand")
                .then(Commands.literal("save")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(ctx -> saveBlueprint(ctx, StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("ghost")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests(BLUEPRINT_SUGGESTIONS)
                                .executes(ctx -> applyGhost(ctx, StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("load")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests(BLUEPRINT_SUGGESTIONS)
                                .executes(ctx -> applyLoad(ctx, StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("give")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests(BLUEPRINT_SUGGESTIONS)
                                .executes(ctx -> giveItem(ctx, StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("clear")
                        .executes(VoxelStandCommands::clearGhost))
                .then(Commands.literal("list")
                        .executes(VoxelStandCommands::listBlueprints));

        dispatcher.register(baseCommand);

        // Алиасы /vs и /customsmith для удобства
        dispatcher.register(Commands.literal("vs").redirect(dispatcher.getRoot().getChild("voxelstand")));
        dispatcher.register(Commands.literal("customsmith").redirect(dispatcher.getRoot().getChild("voxelstand")));
    }

    private static int saveBlueprint(CommandContext<CommandSourceStack> ctx, String name) {
        CommandSourceStack source = ctx.getSource();
        try {
            ServerPlayer player = source.getPlayerOrException();
            SculptStandBlockEntity stand = findTargetStand(player);
            if (stand == null) {
                source.sendFailure(Component.literal("§c[CustomSmith] Посмотрите на скульптурный стапель с предметом или подойдите ближе!"));
                return 0;
            }

            if (stand.getMode() == SculptStandBlockEntity.StandMode.EMPTY) {
                source.sendFailure(Component.literal("§c[CustomSmith] Стапель пуст! Установите предмет перед сохранением."));
                return 0;
            }

            boolean ok = BlueprintManager.saveBlueprint(name, stand, player.getGameProfile().getName());
            if (ok) {
                int voxels = stand.getTotalVoxelCount();
                float quality = stand.calculateQualityMultiplier();
                source.sendSuccess(() -> Component.literal("§a[CustomSmith] Чертеж '§e" + name + "§a' успешно сохранен! (Вокселей: §b" + voxels +
                        "§a, Качество: §6x" + String.format("%.2f", quality) + "§a) в config/customsmith/blueprints/"), true);
                return 1;
            } else {
                source.sendFailure(Component.literal("§c[CustomSmith] Ошибка при сохранении чертежа '" + name + "'."));
                return 0;
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c[CustomSmith] Ошибка: " + e.getMessage()));
            return 0;
        }
    }

    private static int applyGhost(CommandContext<CommandSourceStack> ctx, String name) {
        CommandSourceStack source = ctx.getSource();
        try {
            ServerPlayer player = source.getPlayerOrException();
            SculptStandBlockEntity stand = findTargetStand(player);
            if (stand == null) {
                source.sendFailure(Component.literal("§c[CustomSmith] Посмотрите на скульптурный стапель!"));
                return 0;
            }

            boolean ok = BlueprintManager.applyGhostHologram(stand, name);
            if (ok) {
                source.sendSuccess(() -> Component.literal("§a[CustomSmith] Голограмма чертежа '§e" + name +
                        "§a' активирована на стапеле (прозрачность §b0.7§a). Вы можете ковать по контуру!"), true);
                return 1;
            } else {
                source.sendFailure(Component.literal("§c[CustomSmith] Чертеж '" + name + "' не найден в папке config/customsmith/blueprints/"));
                return 0;
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c[CustomSmith] Ошибка: " + e.getMessage()));
            return 0;
        }
    }

    private static int applyLoad(CommandContext<CommandSourceStack> ctx, String name) {
        CommandSourceStack source = ctx.getSource();
        try {
            ServerPlayer player = source.getPlayerOrException();
            SculptStandBlockEntity stand = findTargetStand(player);
            if (stand == null) {
                source.sendFailure(Component.literal("§c[CustomSmith] Посмотрите на скульптурный стапель!"));
                return 0;
            }

            boolean ok = BlueprintManager.applyCreativeBuild(stand, name);
            if (ok) {
                source.sendSuccess(() -> Component.literal("§a[CustomSmith] Креативная выгрузка чертежа '§e" + name + "§a' на стапель завершена!"), true);
                return 1;
            } else {
                source.sendFailure(Component.literal("§c[CustomSmith] Чертеж '" + name + "' не найден в config/customsmith/blueprints/"));
                return 0;
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c[CustomSmith] Ошибка: " + e.getMessage()));
            return 0;
        }
    }

    private static int giveItem(CommandContext<CommandSourceStack> ctx, String name) {
        CommandSourceStack source = ctx.getSource();
        try {
            ServerPlayer player = source.getPlayerOrException();
            ItemStack item = BlueprintManager.createItemFromBlueprint(name);
            if (item.isEmpty()) {
                source.sendFailure(Component.literal("§c[CustomSmith] Чертеж '" + name + "' не найден или поврежден!"));
                return 0;
            }

            if (!player.addItem(item)) {
                player.drop(item, false);
            }
            source.sendSuccess(() -> Component.literal("§a[CustomSmith] Выкованный предмет '§e" + name + "§a' выдан в ваш инвентарь!"), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c[CustomSmith] Ошибка: " + e.getMessage()));
            return 0;
        }
    }

    private static int clearGhost(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        try {
            ServerPlayer player = source.getPlayerOrException();
            SculptStandBlockEntity stand = findTargetStand(player);
            if (stand == null) {
                source.sendFailure(Component.literal("§c[CustomSmith] Посмотрите на скульптурный стапель!"));
                return 0;
            }

            stand.clearGhostBlueprint();
            source.sendSuccess(() -> Component.literal("§e[CustomSmith] Голограмма чертежа удалена со стапеля."), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c[CustomSmith] Ошибка: " + e.getMessage()));
            return 0;
        }
    }

    private static int listBlueprints(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        List<String> list = BlueprintManager.listBlueprints();
        if (list.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§7[CustomSmith] В папке config/customsmith/blueprints/ пока нет сохраненных чертежей."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("§6=== [ Чертежи CustomSmith (" + list.size() + ") ] ==="), false);
        for (String bp : list) {
            source.sendSuccess(() -> Component.literal("§b✦ §f" + bp), false);
        }
        return list.size();
    }

    /**
     * Поиск стапеля: сначала по лучу взгляда (до 5 блоков), затем в радиусе 3 блоков от игрока.
     */
    private static SculptStandBlockEntity findTargetStand(ServerPlayer player) {
        Level level = player.level();
        HitResult hit = player.pick(5.0D, 0.0F, false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = ((BlockHitResult) hit).getBlockPos();
            BlockEntity be = level.getBlockEntity(hitPos);
            if (be instanceof SculptStandBlockEntity stand) {
                return stand;
            }
        }

        // Поиск в радиусе 3 блоков
        BlockPos playerPos = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(playerPos.offset(-3, -2, -3), playerPos.offset(3, 2, 3))) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SculptStandBlockEntity stand) {
                return stand;
            }
        }
        return null;
    }
}
