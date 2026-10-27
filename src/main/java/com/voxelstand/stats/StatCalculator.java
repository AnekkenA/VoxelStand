package com.voxelstand.stats;

import com.voxelstand.voxel.VoxelGrid;
import com.voxelstand.voxel.VoxelMaterial;

import java.util.Map;

/**
 * Калькулятор динамических RPG-характеристик оружия, брони и инструментов.
 */
public final class StatCalculator {
    private StatCalculator() {}

    public static final float[] MATERIAL_DENSITY = new float[32];
    public static final float[] MATERIAL_DURABILITY = new float[32];

    static {
        // Плотность (вес на 1 воксель)
        MATERIAL_DENSITY[VoxelMaterial.GOLD.getId()] = 1.8f;        // Золото: плотный тяжелый балласт для кувалд
        MATERIAL_DENSITY[VoxelMaterial.DEEPSLATE.getId()] = 1.5f;   // Глубинный сланец: плотная каменная сердцевина
        MATERIAL_DENSITY[VoxelMaterial.OBSIDIAN.getId()] = 1.4f;    // Обсидиан: тяжелое вулканическое стекло
        MATERIAL_DENSITY[VoxelMaterial.IRON.getId()] = 1.2f;        // Железо: стандартная сталь
        MATERIAL_DENSITY[VoxelMaterial.COPPER.getId()] = 1.1f;      // Медь
        MATERIAL_DENSITY[VoxelMaterial.FLINT.getId()] = 1.0f;       // Кремень
        MATERIAL_DENSITY[VoxelMaterial.NETHERITE.getId()] = 1.0f;   // Незерит: сбалансированный титановый сплав
        MATERIAL_DENSITY[VoxelMaterial.DARK_PRISMARINE.getId()] = 1.1f;
        MATERIAL_DENSITY[VoxelMaterial.ECHO_SHARD.getId()] = 1.2f;
        MATERIAL_DENSITY[VoxelMaterial.SHULKER.getId()] = 1.0f;
        MATERIAL_DENSITY[VoxelMaterial.AMETHYST.getId()] = 0.9f;
        MATERIAL_DENSITY[VoxelMaterial.EMERALD.getId()] = 0.9f;
        MATERIAL_DENSITY[VoxelMaterial.PURPUR.getId()] = 0.9f;
        MATERIAL_DENSITY[VoxelMaterial.DIAMOND.getId()] = 0.8f;     // Алмаз: легкий прочный кристалл
        MATERIAL_DENSITY[VoxelMaterial.QUARTZ.getId()] = 0.8f;      // Кварц
        MATERIAL_DENSITY[VoxelMaterial.LAPIS.getId()] = 0.8f;
        MATERIAL_DENSITY[VoxelMaterial.SEA_LANTERN.getId()] = 0.8f;
        MATERIAL_DENSITY[VoxelMaterial.BLAZE.getId()] = 0.7f;
        MATERIAL_DENSITY[VoxelMaterial.REDSTONE.getId()] = 0.7f;
        MATERIAL_DENSITY[VoxelMaterial.BONE.getId()] = 0.6f;
        MATERIAL_DENSITY[VoxelMaterial.GLOWSTONE.getId()] = 0.6f;
        MATERIAL_DENSITY[VoxelMaterial.DARK_WOOD.getId()] = 0.5f;
        MATERIAL_DENSITY[VoxelMaterial.MANGROVE.getId()] = 0.4f;
        MATERIAL_DENSITY[VoxelMaterial.ACACIA.getId()] = 0.4f;
        MATERIAL_DENSITY[VoxelMaterial.CRIMSON.getId()] = 0.4f;
        MATERIAL_DENSITY[VoxelMaterial.OAK.getId()] = 0.4f;
        MATERIAL_DENSITY[VoxelMaterial.JUNGLE.getId()] = 0.4f;
        MATERIAL_DENSITY[VoxelMaterial.CHERRY.getId()] = 0.4f;
        MATERIAL_DENSITY[VoxelMaterial.WARPED.getId()] = 0.4f;
        MATERIAL_DENSITY[VoxelMaterial.LIGHT_WOOD.getId()] = 0.25f; // Береза/Бамбук: ультралегкие для рапир и катан
        MATERIAL_DENSITY[VoxelMaterial.LEATHER.getId()] = 0.2f;

        // Вклад в прочность на 1 воксель (1 слиток = 128 вокселей)
        MATERIAL_DURABILITY[VoxelMaterial.NETHERITE.getId()] = 6.0f;   // 128 вокс = +768 HP
        MATERIAL_DURABILITY[VoxelMaterial.OBSIDIAN.getId()] = 4.5f;    // 128 вокс = +576 HP
        MATERIAL_DURABILITY[VoxelMaterial.DIAMOND.getId()] = 4.0f;     // 128 вокс = +512 HP
        MATERIAL_DURABILITY[VoxelMaterial.ECHO_SHARD.getId()] = 3.0f;
        MATERIAL_DURABILITY[VoxelMaterial.IRON.getId()] = 1.5f;        // 128 вокс = +192 HP
        MATERIAL_DURABILITY[VoxelMaterial.AMETHYST.getId()] = 1.5f;
        MATERIAL_DURABILITY[VoxelMaterial.SHULKER.getId()] = 1.5f;
        MATERIAL_DURABILITY[VoxelMaterial.DARK_PRISMARINE.getId()] = 1.5f;
        MATERIAL_DURABILITY[VoxelMaterial.COPPER.getId()] = 1.0f;      // 128 вокс = +128 HP
        MATERIAL_DURABILITY[VoxelMaterial.QUARTZ.getId()] = 1.0f;
        MATERIAL_DURABILITY[VoxelMaterial.BLAZE.getId()] = 1.0f;
        MATERIAL_DURABILITY[VoxelMaterial.EMERALD.getId()] = 1.0f;
        MATERIAL_DURABILITY[VoxelMaterial.PURPUR.getId()] = 1.0f;
        MATERIAL_DURABILITY[VoxelMaterial.DEEPSLATE.getId()] = 0.8f;
        MATERIAL_DURABILITY[VoxelMaterial.FLINT.getId()] = 0.6f;
        MATERIAL_DURABILITY[VoxelMaterial.BONE.getId()] = 0.6f;
        MATERIAL_DURABILITY[VoxelMaterial.DARK_WOOD.getId()] = 0.5f;
        MATERIAL_DURABILITY[VoxelMaterial.LAPIS.getId()] = 0.5f;
        MATERIAL_DURABILITY[VoxelMaterial.SEA_LANTERN.getId()] = 0.5f;
        MATERIAL_DURABILITY[VoxelMaterial.OAK.getId()] = 0.4f;
        MATERIAL_DURABILITY[VoxelMaterial.MANGROVE.getId()] = 0.4f;
        MATERIAL_DURABILITY[VoxelMaterial.ACACIA.getId()] = 0.4f;
        MATERIAL_DURABILITY[VoxelMaterial.JUNGLE.getId()] = 0.4f;
        MATERIAL_DURABILITY[VoxelMaterial.CHERRY.getId()] = 0.4f;
        MATERIAL_DURABILITY[VoxelMaterial.CRIMSON.getId()] = 0.4f;
        MATERIAL_DURABILITY[VoxelMaterial.WARPED.getId()] = 0.4f;
        MATERIAL_DURABILITY[VoxelMaterial.LIGHT_WOOD.getId()] = 0.3f;
        MATERIAL_DURABILITY[VoxelMaterial.GOLD.getId()] = 0.3f;        // Золото: мягкое/хрупкое
        MATERIAL_DURABILITY[VoxelMaterial.LEATHER.getId()] = 0.3f;
        MATERIAL_DURABILITY[VoxelMaterial.REDSTONE.getId()] = 0.3f;
        MATERIAL_DURABILITY[VoxelMaterial.GLOWSTONE.getId()] = 0.3f;
    }

    public static ItemStatData calculate(Map<String, VoxelGrid> grids, boolean isArmor) {
        return calculate(grids, isArmor, net.minecraft.world.item.ItemStack.EMPTY);
    }

    public static ItemStatData calculate(Map<String, VoxelGrid> grids, net.minecraft.world.item.ItemStack stack) {
        boolean isArmor = stack.getItem() instanceof net.minecraft.world.item.ArmorItem;
        return calculate(grids, isArmor, stack);
    }

    public static ItemStatData calculate(Map<String, VoxelGrid> grids, boolean isArmor, net.minecraft.world.item.ItemStack stack) {
        float quality = 1.0f;
        if (com.voxelstand.api.VoxelDataHelper.hasCustomKey(stack, "QualityModifier")) {
            quality = com.voxelstand.api.VoxelDataHelper.getCustomData(stack).getFloat("QualityModifier");
        }
        return calculate(grids, isArmor, stack, quality);
    }

    public static ItemStatData calculate(Map<String, VoxelGrid> grids, boolean isArmor, net.minecraft.world.item.ItemStack stack, float qualityMultiplier) {
        ItemStatData data = new ItemStatData();
        int[] counts = new int[32];
        int totalVoxels = 0;
        int minX = 32, maxX = -1, minY = 32, maxY = -1, minZ = 32, maxZ = -1;

        for (VoxelGrid grid : grids.values()) {
            byte[] raw = grid.getRawData();
            for (int idx = 0; idx < raw.length; idx++) {
                int id = raw[idx] & 0x1F;
                if (id != 0) {
                    counts[id]++;
                    totalVoxels++;
                    int z = idx & 31;
                    int y = (idx >> 5) & 31;
                    int x = (idx >> 10) & 31;
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                    if (z < minZ) minZ = z;
                    if (z > maxZ) maxZ = z;
                }
            }
        }

        int spanX = totalVoxels > 0 ? (maxX - minX + 1) : 0;
        int spanY = totalVoxels > 0 ? (maxY - minY + 1) : 0;
        int spanZ = totalVoxels > 0 ? (maxZ - minZ + 1) : 0;
        data.totalVoxels = totalVoxels;

        // 1. Определение доминантного материала
        int maxCount = 0;
        byte dominantMat = VoxelMaterial.OAK.getId();
        for (int i = 1; i < 32; i++) {
            if (counts[i] > maxCount) {
                maxCount = counts[i];
                dominantMat = (byte) i;
            }
        }
        data.dominantMaterialId = dominantMat;

        // 2. Правило декоративной инкрустации (<15% объема и <=64 вокселей):
        // Снижает вклад в физический вес до 0.1x, не ломая сборку и сохраняя зачарования
        float totalWeight = 0;
        float totalDurabilityBonus = 0;
        int inlayVoxels = 0;
        int maxAllowedInlay = Math.max(1, (int) (totalVoxels * 0.25f)); // Не более 25% суммарных инкрустаций

        for (int i = 1; i < 32; i++) {
            int c = counts[i];
            if (c > 0) {
                float ratio = (float) c / (float) Math.max(1, totalVoxels);
                boolean canBeInlay = (ratio < 0.15f) && (c <= 64) && (i != dominantMat) && ((inlayVoxels + c) <= maxAllowedInlay);
                float densityMult = canBeInlay ? 0.10f : 1.0f;

                if (canBeInlay) {
                    inlayVoxels += c;
                }

                totalWeight += c * MATERIAL_DENSITY[i] * densityMult;
                totalDurabilityBonus += c * MATERIAL_DURABILITY[i];
            }
        }

        data.weight = totalWeight;
        data.totalWeight = totalWeight;
        data.inlayRatio = totalVoxels > 0 ? ((float) inlayVoxels / (float) totalVoxels) : 0.0f;

        int baseDurability = 100;
        int maxAllowedDurability = 3200; // Предотвращает поломку наковальни ("Too Expensive")

        boolean isBowOrCrossbow = !stack.isEmpty() && (stack.getItem() instanceof net.minecraft.world.item.BowItem || stack.getItem() instanceof net.minecraft.world.item.CrossbowItem);

        if (!stack.isEmpty()) {
            if (stack.getItem() instanceof net.minecraft.world.item.ShieldItem) {
                baseDurability = 336;
                maxAllowedDurability = 1500;
            } else if (isBowOrCrossbow) {
                baseDurability = 384;
                maxAllowedDurability = 1500;
            } else if (isArmor) {
                baseDurability = 150;
            }
        }

        data.maxDurability = Math.min(maxAllowedDurability, baseDurability + (int) totalDurabilityBonus);
        data.isBroken = false;

        // 3. Скорость атаки (тонкая катана весом ~200 быстрая 1.7..1.9, тяжелая кувалда весом ~800 медленная 0.65..0.75)
        float speedCalc = 2.2f - Math.max(0.0f, totalWeight - 100.0f) / 460.0f;
        data.attackSpeed = Math.max(0.65f, Math.min(2.4f, speedCalc));

        // Штраф к скорости передвижения: максимум -6.5% на 1 деталь (до -26% за полный комплект)
        data.movementSpeedPenalty = -Math.min(0.065f, totalWeight / 8000.0f);
        data.miningSpeedModifier = Math.max(0.5f, 1.2f - (totalWeight / 1500.0f));

        // 4. Уровень копания (Mining Tier)
        if (counts[VoxelMaterial.NETHERITE.getId()] >= 80) {
            data.miningTier = 4;
        } else if (counts[VoxelMaterial.DIAMOND.getId()] >= 60) {
            data.miningTier = 3;
        } else if (counts[VoxelMaterial.IRON.getId()] >= 50) {
            data.miningTier = 2;
        } else if (counts[VoxelMaterial.COPPER.getId()] >= 40) {
            data.miningTier = 1;
        } else {
            data.miningTier = 0;
        }

        float massBonus = 0.0f;

        if (isArmor) {
            // 5. Характеристики брони с поэлементными капами
            float maxSlotArmor = 8.0f;
            float maxSlotToughness = 3.0f;

            if (stack.getItem() instanceof net.minecraft.world.item.ArmorItem armorItem) {
                switch (armorItem.getEquipmentSlot()) {
                    case HEAD -> { maxSlotArmor = 4.0f; maxSlotToughness = 3.0f; }
                    case CHEST -> { maxSlotArmor = 10.0f; maxSlotToughness = 4.0f; }
                    case LEGS -> { maxSlotArmor = 8.0f; maxSlotToughness = 3.0f; }
                    case FEET -> { maxSlotArmor = 4.0f; maxSlotToughness = 3.0f; }
                    default -> {}
                }
            }

            float rawArmor = (counts[VoxelMaterial.IRON.getId()] * 0.020f) +
                             (counts[VoxelMaterial.COPPER.getId()] * 0.015f) +
                             (counts[VoxelMaterial.DIAMOND.getId()] * 0.025f) +
                             (counts[VoxelMaterial.NETHERITE.getId()] * 0.025f);

            data.armor = Math.max(1.0f, Math.min(maxSlotArmor, rawArmor));

            float rawToughness = (counts[VoxelMaterial.DIAMOND.getId()] * 0.015f) +
                                 (counts[VoxelMaterial.NETHERITE.getId()] * 0.015f);
            data.armorToughness = Math.min(maxSlotToughness, rawToughness);

            data.knockbackResistance = Math.min(0.25f, counts[VoxelMaterial.NETHERITE.getId()] * 0.0015f);
            data.damage = 0;
        } else if (!stack.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.ShieldItem) {
            // Щит
            data.armor = 0;
            data.armorToughness = 0;
            data.damage = 0;
            data.knockbackResistance = Math.min(0.40f, (totalWeight / 1500.0f) + (counts[VoxelMaterial.OBSIDIAN.getId()] * 0.002f));
        } else if (isBowOrCrossbow) {
            // 6. Физика и металлургия стрелкового оружия (Луки и Арбалеты)
            boolean isCrossbow = stack.getItem() instanceof net.minecraft.world.item.CrossbowItem;

            // Упругие материалы: бамбук, светлое дерево, дуб, кожа, кремень
            int elasticVoxels = counts[VoxelMaterial.LIGHT_WOOD.getId()] * 2 +
                                counts[VoxelMaterial.OAK.getId()] +
                                counts[VoxelMaterial.CHERRY.getId()] +
                                counts[VoxelMaterial.ACACIA.getId()] +
                                counts[VoxelMaterial.JUNGLE.getId()] +
                                counts[VoxelMaterial.LEATHER.getId()] +
                                counts[VoxelMaterial.FLINT.getId()];

            // Жесткие/тяжелые композитные материалы: незерит, железо, кость, сланец
            int rigidVoxels = counts[VoxelMaterial.NETHERITE.getId()] * 2 +
                              counts[VoxelMaterial.IRON.getId()] +
                              counts[VoxelMaterial.BONE.getId()] +
                              counts[VoxelMaterial.DEEPSLATE.getId()] +
                              counts[VoxelMaterial.OBSIDIAN.getId()];

            // Базовое время натяжения: 20 тиков для лука (1.0с), 25 тиков для арбалета (1.25с)
            float baseDraw = isCrossbow ? 25.0f : 20.0f;
            float drawMod = (rigidVoxels * 0.08f) - (elasticVoxels * 0.06f);
            data.drawSpeedTicks = Math.max(10.0f, Math.min(38.0f, baseDraw + drawMod));

            // Скорость полета стрелы (кинетический импульс жестких композитных плеч)
            float velocityBonus = (counts[VoxelMaterial.NETHERITE.getId()] * 0.005f) +
                                  (counts[VoxelMaterial.IRON.getId()] * 0.003f) +
                                  (counts[VoxelMaterial.BONE.getId()] * 0.0025f) +
                                  (counts[VoxelMaterial.DEEPSLATE.getId()] * 0.002f);
            data.arrowVelocityMult = Math.max(0.85f, Math.min(2.2f, 1.0f + velocityBonus));

            // Встроенное бронепробитие
            float rawPenetration = (counts[VoxelMaterial.NETHERITE.getId()] * 0.003f) +
                                   (counts[VoxelMaterial.IRON.getId()] * 0.0015f) +
                                   (counts[VoxelMaterial.FLINT.getId()] * 0.0015f) +
                                   (counts[VoxelMaterial.DEEPSLATE.getId()] * 0.001f);
            data.armorPenetration = Math.max(0.0f, Math.min(0.60f, rawPenetration));

            // Дрожание прицела (Aim Sway) при долгом натяжении тяжелых луков
            if (totalWeight > 300.0f) {
                data.aimSway = Math.min(3.5f, (totalWeight - 300.0f) / 150.0f);
            } else {
                data.aimSway = 0.0f;
            }

            data.damage = isCrossbow ? 9.0f : 6.0f;
            data.damage += (data.arrowVelocityMult - 1.0f) * 4.0f;
            data.armor = 0;
            data.armorToughness = 0;
            data.knockbackResistance = 0;
        } else {
            // 7. Характеристики оружия ближнего боя / инструментов
            float flintDmg = Math.min(2.0f, counts[VoxelMaterial.FLINT.getId()] * 0.025f);
            float copperDmg = Math.min(3.0f, counts[VoxelMaterial.COPPER.getId()] * 0.030f);
            float ironDmg = Math.min(4.0f, counts[VoxelMaterial.IRON.getId()] * 0.035f);
            float diamondDmg = Math.min(5.0f, counts[VoxelMaterial.DIAMOND.getId()] * 0.035f);
            float netheriteDmg = Math.min(6.0f, counts[VoxelMaterial.NETHERITE.getId()] * 0.035f);
            float goldDmg = Math.min(2.0f, counts[VoxelMaterial.GOLD.getId()] * 0.020f);
            float obsidianDmg = Math.min(3.5f, counts[VoxelMaterial.OBSIDIAN.getId()] * 0.025f);

            // Потолок определяется наивысшим присутствующим металлом/кристаллом
            float maxCeiling = 2.0f;
            if (counts[VoxelMaterial.NETHERITE.getId()] > 0) maxCeiling = 6.0f;
            else if (counts[VoxelMaterial.DIAMOND.getId()] > 0) maxCeiling = 5.0f;
            else if (counts[VoxelMaterial.IRON.getId()] > 0 || counts[VoxelMaterial.OBSIDIAN.getId()] > 0) maxCeiling = 4.0f;
            else if (counts[VoxelMaterial.COPPER.getId()] > 0) maxCeiling = 3.0f;

            float metalSum = flintDmg + copperDmg + ironDmg + diamondDmg + netheriteDmg + goldDmg + obsidianDmg;
            float materialBonus = Math.min(maxCeiling, metalSum);

            // Сокрушающий бонус кинетической массы (для тяжелых топоров, кувалд и булав > 350 веса)
            if (totalWeight > 350.0f) {
                massBonus = Math.min(3.0f, (totalWeight - 350.0f) / 200.0f);
            }

            // Защита от "золотой зубочистки": крошечное оружие не получает кинетический бонус массы
            if (spanY <= 12 && totalVoxels < 200) {
                massBonus = 0.0f;
            }

            data.damage = 2.0f + materialBonus + massBonus;
            data.armor = 0;
            data.armorToughness = 0;
            data.knockbackResistance = 0;
        }

        // 8. Определение весового класса с силуэтным коридором (защита от визуального обмана)
        ItemStatData.WeightClass wClass;
        if (totalWeight < 250.0f) {
            wClass = ItemStatData.WeightClass.LIGHT;
        } else if (totalWeight < 450.0f) {
            wClass = ItemStatData.WeightClass.MEDIUM;
        } else {
            wClass = ItemStatData.WeightClass.HEAVY;
        }

        if (!isArmor) {
            // Для оружия:
            // 1. Длинный или массивный клинок не может быть легким
            if (spanY >= 26 || totalVoxels >= 500) {
                if (wClass == ItemStatData.WeightClass.LIGHT) {
                    wClass = ItemStatData.WeightClass.MEDIUM;
                }
            }
            // 2. Колоссальный двуручник / осадный молот (размах >= 30 и вокселей >= 800)
            if (spanY >= 30 && totalVoxels >= 800) {
                wClass = ItemStatData.WeightClass.HEAVY;
            }
            // 3. Зубочистка: крошечный кинжал не может быть тяжелым
            if (spanY <= 12 && totalVoxels < 200 && wClass == ItemStatData.WeightClass.HEAVY) {
                wClass = ItemStatData.WeightClass.MEDIUM;
            }
        } else {
            // Для брони: массивная кираса не может быть легкой
            if (totalVoxels >= 900 || spanZ >= 12) {
                if (wClass == ItemStatData.WeightClass.LIGHT) {
                    wClass = ItemStatData.WeightClass.MEDIUM;
                }
            }
            if (totalVoxels >= 1400) {
                wClass = ItemStatData.WeightClass.HEAVY;
            }
        }
        data.weightClass = wClass;

        // 6. Начисление зачарований от материалов
        if (!stack.isEmpty()) {
            MaterialEnchantmentManager.applyEnchantments(counts, data, stack);
        } else {
            MaterialEnchantmentManager.applyEnchantments(counts, data, isArmor);
        }

        // 7. Применение модификатора качества пластин (K_quality)
        if (qualityMultiplier > 0.0f && qualityMultiplier != 1.0f) {
            data.qualityMultiplier = qualityMultiplier;
            data.damage *= qualityMultiplier;
            data.attackSpeed *= qualityMultiplier;
            data.maxDurability = Math.max(1, (int) (data.maxDurability * qualityMultiplier));
            data.armor *= qualityMultiplier;
            data.armorToughness *= qualityMultiplier;
            data.knockbackResistance *= qualityMultiplier;
            data.arrowVelocityMult *= qualityMultiplier;
            data.armorPenetration = Math.min(0.85f, data.armorPenetration * qualityMultiplier);

            if (qualityMultiplier >= 1.25f) data.qualityName = "Чистое";
            else if (qualityMultiplier >= 1.10f) data.qualityName = "Крепкое";
            else data.qualityName = "Обычное";
        }

        return data;
    }
}
