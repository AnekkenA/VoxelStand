package com.voxelstand.client.gui;

import com.voxelstand.api.VoxelConstants;
import com.voxelstand.block.entity.SculptStandBlockEntity;
import com.voxelstand.stats.ItemStatData;
import com.voxelstand.stats.StatCalculator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Информационный HUD в реальном времени при наведении взгляда на стапель кузницы (Minecraft 1.21.1 NeoForge).
 * Отображает лимит вокселей, весовой класс, урон/броню/баллистику и характеристики предметов.
 */
public class SculptStandHudOverlay implements LayeredDraw.Layer {

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null || mc.options.hideGui) return;

        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        if (!(mc.level.getBlockEntity(blockHit.getBlockPos()) instanceof SculptStandBlockEntity stand)) {
            return;
        }

        if (stand.getMode() == SculptStandBlockEntity.StandMode.EMPTY || stand.getHeldItem().isEmpty()) {
            return;
        }

        ItemStack held = stand.getHeldItem();
        ItemStatData stats = StatCalculator.calculate(stand.getAllGrids(), held);
        int totalVoxels = stand.getTotalVoxelCount();
        Font font = mc.font;

        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        // Позиционирование карточки в правом верхнем углу
        int cardWidth = 205;
        int lineCount = 8;
        int cardHeight = 26 + (lineCount * 11);
        int cardX = screenWidth - cardWidth - 12;
        int cardY = 12;

        // Полупрозрачный фон и рамка
        graphics.fill(cardX, cardY, cardX + cardWidth, cardY + cardHeight, 0xDD10121A);
        graphics.renderOutline(cardX, cardY, cardWidth, cardHeight, 0xFF4B5568);

        int textX = cardX + 8;
        int textY = cardY + 7;

        // 1. Заголовок предмета и режим работы
        String itemName = held.getHoverName().getString();
        if (itemName.length() > 22) itemName = itemName.substring(0, 20) + "..";
        graphics.drawString(font, "✦ §e" + itemName, textX, textY, 0xFFFFFF, true);
        textY += 11;

        boolean cosmetic = stand.isCosmeticMode() || !com.voxelstand.VoxelStandConfig.CONFIG.enableStatCalculation.get();
        if (cosmetic) {
            graphics.drawString(font, "§b[Режим: Косметика] §7(Статы сохранены)", textX, textY, 0x55FFFF, true);
        } else {
            graphics.drawString(font, "§6[Режим: Ковка] §7(Перерасчет статов)", textX, textY, 0xFFA500, true);
        }
        textY += 12;

        // 2. Лимит вокселей и шкала прогресса
        int maxVox = VoxelConstants.DEFAULT_VOXEL_LIMIT;
        float voxelFraction = Math.min(1.0f, (float) totalVoxels / (float) maxVox);
        graphics.drawString(font, "§7Воксели: §f" + totalVoxels + " §8/ §7" + maxVox, textX, textY, 0xCCCCCC, true);
        textY += 10;

        int barW = cardWidth - 16;
        int barH = 3;
        int barColor = voxelFraction >= 1.0f ? 0xFFFF4444 : (voxelFraction > 0.8f ? 0xFFFFAA00 : 0xFF55FF55);
        graphics.fill(textX, textY, textX + barW, textY + barH, 0xFF222530);
        graphics.fill(textX, textY, textX + (int) (barW * voxelFraction), textY + barH, barColor);
        textY += 6;

        // 3. Весовой класс и вес
        String badge = stats.weightClass.getFormattedBadge();
        String weightStr = String.format("§7Вес: §f%.1f", stats.weight);
        if (stats.inlayRatio > 0.0f) {
            weightStr += String.format(" §8(Инкр: %d%%)", (int) (stats.inlayRatio * 100));
        }
        graphics.drawString(font, "§7Класс: " + badge + " " + weightStr, textX, textY, 0xFFFFFF, true);
        textY += 11;

        // 4. Качество ковки
        float qualityMult = stand.calculateQualityMultiplier();
        String qualityName = stand.getQualityName();
        String qualityStr = String.format("§7Качество: §6%s §8(x%.2f)", qualityName, qualityMult);
        graphics.drawString(font, qualityStr, textX, textY, 0xFFFFFF, true);
        textY += 11;

        // 5. Боевые параметры в зависимости от типа предмета
        if (held.getItem() instanceof BowItem || held.getItem() instanceof CrossbowItem) {
            String drawStr = String.format("§7Натяг: §b%.2fs §8(%dт) §7Скор: §a+%d%%",
                    stats.drawSpeedTicks / 20.0f, (int) stats.drawSpeedTicks, (int) ((stats.arrowVelocityMult - 1.0f) * 100));
            graphics.drawString(font, drawStr, textX, textY, 0xFFFFFF, true);
            textY += 11;

            String penStr = String.format("§7Бронепробитие: §6%d%%  §7ХП: §a%d",
                    (int) (stats.armorPenetration * 100), stats.maxDurability);
            graphics.drawString(font, penStr, textX, textY, 0xFFFFFF, true);
        } else if (held.getItem() instanceof ShieldItem) {
            String shieldStr = String.format("§7Сопр. толчку: §6+%d%%  §7ХП: §a%d",
                    (int) (stats.knockbackResistance * 100), stats.maxDurability);
            graphics.drawString(font, shieldStr, textX, textY, 0xFFFFFF, true);
        } else if (held.getItem() instanceof net.minecraft.world.item.ArmorItem) {
            String armorStr = String.format("§7Защита: §b+%.1f  §7Стойкость: §9+%.1f",
                    stats.armor, stats.armorToughness);
            graphics.drawString(font, armorStr, textX, textY, 0xFFFFFF, true);
            textY += 11;

            String penaltyStr = String.format("§7Штраф бега: §c-%.1f%%  §7ХП: §a%d",
                    Math.abs(stats.movementSpeedPenalty * 100), stats.maxDurability);
            graphics.drawString(font, penaltyStr, textX, textY, 0xFFFFFF, true);
        } else {
            String combatStr = String.format("§7Урон: §c+%.1f  §7Скор. атаки: §b%.2f",
                    stats.damage, stats.attackSpeed);
            graphics.drawString(font, combatStr, textX, textY, 0xFFFFFF, true);
            textY += 11;

            String durStr = String.format("§7Прочность: §a%d  §7Траншея: §eT%d",
                    stats.maxDurability, stats.miningTier);
            graphics.drawString(font, durStr, textX, textY, 0xFFFFFF, true);
        }

        textY += 13;
        graphics.drawString(font, "§8[ПКМ рукой: Режим | Shift: Снять]", textX, textY, 0x888888, false);
    }
}
