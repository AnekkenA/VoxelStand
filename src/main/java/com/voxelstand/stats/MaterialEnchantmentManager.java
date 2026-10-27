package com.voxelstand.stats;

import com.voxelstand.voxel.VoxelMaterial;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Менеджер назначения строго оригинальных ванильных зачарований
 * на основе порогов использованных вокселей материалов.
 */
public final class MaterialEnchantmentManager {
    private MaterialEnchantmentManager() {}

    public static void applyEnchantments(int[] counts, ItemStatData data, net.minecraft.world.item.ItemStack stack) {
        if (stack.getItem() instanceof net.minecraft.world.item.BowItem) {
            // Лук: Пламя, Бесконечность, Сила, Откидывание
            int blaze = counts[VoxelMaterial.BLAZE.getId()];
            if (blaze >= 15) data.enchantments.put(Enchantments.FLAMING_ARROWS, 1);

            int lapis = counts[VoxelMaterial.LAPIS.getId()];
            if (lapis >= 40) data.enchantments.put(Enchantments.INFINITY_ARROWS, 1);

            int quartz = counts[VoxelMaterial.QUARTZ.getId()];
            int powerLvl = Math.min(5, quartz / 25);
            if (powerLvl > 0) data.enchantments.put(Enchantments.POWER_ARROWS, powerLvl);

            int deepslate = counts[VoxelMaterial.DEEPSLATE.getId()];
            if (deepslate >= 30) data.enchantments.put(Enchantments.PUNCH_ARROWS, 2);

            if (counts[VoxelMaterial.ECHO_SHARD.getId()] >= 30) {
                data.enchantments.put(Enchantments.MENDING, 1);
            }
            return;
        }

        if (stack.getItem() instanceof net.minecraft.world.item.CrossbowItem) {
            // Арбалет: Пронзающая стрела, Быстрая перезарядка, Тройной выстрел
            int shulker = counts[VoxelMaterial.SHULKER.getId()];
            int pierceLvl = Math.min(4, shulker / 25);
            if (pierceLvl > 0) data.enchantments.put(Enchantments.PIERCING, pierceLvl);

            int redstone = counts[VoxelMaterial.REDSTONE.getId()];
            int quickLvl = Math.min(3, redstone / 25);
            if (quickLvl > 0) data.enchantments.put(Enchantments.QUICK_CHARGE, quickLvl);

            int quartz = counts[VoxelMaterial.QUARTZ.getId()];
            if (quartz >= 40) data.enchantments.put(Enchantments.MULTISHOT, 1);

            if (counts[VoxelMaterial.ECHO_SHARD.getId()] >= 30) {
                data.enchantments.put(Enchantments.MENDING, 1);
            }
            return;
        }

        if (stack.getItem() instanceof net.minecraft.world.item.ShieldItem) {
            // Щит: Шипы, Прочность
            int amethyst = counts[VoxelMaterial.AMETHYST.getId()];
            int thornsLvl = Math.min(3, amethyst / 20);
            if (thornsLvl > 0) data.enchantments.put(Enchantments.THORNS, thornsLvl);

            if (counts[VoxelMaterial.OBSIDIAN.getId()] >= 35) {
                data.enchantments.put(Enchantments.UNBREAKING, 3);
            }

            if (counts[VoxelMaterial.ECHO_SHARD.getId()] >= 30) {
                data.enchantments.put(Enchantments.MENDING, 1);
            }
            return;
        }

        boolean isArmor = stack.getItem() instanceof net.minecraft.world.item.ArmorItem;
        applyEnchantments(counts, data, isArmor);
    }

    public static void applyEnchantments(int[] counts, ItemStatData data, boolean isArmor) {
        // 1. Огонь (Стержень ифрита)
        int blaze = counts[VoxelMaterial.BLAZE.getId()];
        if (blaze >= 30) {
            if (isArmor) data.enchantments.put(Enchantments.FIRE_PROTECTION, 2);
            else data.enchantments.put(Enchantments.FIRE_ASPECT, 2);
        } else if (blaze >= 15) {
            if (isArmor) data.enchantments.put(Enchantments.FIRE_PROTECTION, 1);
            else data.enchantments.put(Enchantments.FIRE_ASPECT, 1);
        }

        // 2. Удача / Добыча (Лазурит)
        int lapis = counts[VoxelMaterial.LAPIS.getId()];
        if (lapis >= 60) {
            if (!isArmor) data.enchantments.put(Enchantments.BLOCK_FORTUNE, 3);
        } else if (lapis >= 40) {
            if (!isArmor) data.enchantments.put(Enchantments.BLOCK_FORTUNE, 2);
        } else if (lapis >= 20) {
            if (!isArmor) data.enchantments.put(Enchantments.BLOCK_FORTUNE, 1);
        }

        // 3. Заточка / Эффективность (Кварц)
        int quartz = counts[VoxelMaterial.QUARTZ.getId()];
        int quartzLevel = Math.min(5, quartz / 25);
        if (quartzLevel > 0 && !isArmor) {
            data.enchantments.put(Enchantments.SHARPNESS, quartzLevel);
            data.enchantments.put(Enchantments.BLOCK_EFFICIENCY, quartzLevel);
        }

        // 4. Защита (Железо и Алмаз)
        if (isArmor) {
            int iron = counts[VoxelMaterial.IRON.getId()];
            int protLvl = Math.min(4, iron / 40);
            if (counts[VoxelMaterial.DIAMOND.getId()] >= 80) protLvl = 4;
            if (protLvl > 0) {
                data.enchantments.put(Enchantments.ALL_DAMAGE_PROTECTION, protLvl);
            }
        }

        // 5. Взрывоустойчивость (Обсидиан)
        if (isArmor && counts[VoxelMaterial.OBSIDIAN.getId()] >= 35) {
            data.enchantments.put(Enchantments.BLAST_PROTECTION, 2);
        }

        // 6. Защита от снарядов / Шелковое касание (Шалкер)
        int shulker = counts[VoxelMaterial.SHULKER.getId()];
        if (shulker >= 25) {
            if (isArmor) data.enchantments.put(Enchantments.PROJECTILE_PROTECTION, 2);
            else data.enchantments.put(Enchantments.SILK_TOUCH, 1);
        }

        // 7. Шипы (Осколок аметиста)
        if (isArmor) {
            int amethyst = counts[VoxelMaterial.AMETHYST.getId()];
            int thornsLvl = Math.min(3, amethyst / 20);
            if (thornsLvl > 0) {
                data.enchantments.put(Enchantments.THORNS, thornsLvl);
            }
        }

        // 8. Подводное дыхание и Подводник (Морской фонарь)
        if (isArmor && counts[VoxelMaterial.SEA_LANTERN.getId()] >= 25) {
            data.enchantments.put(Enchantments.RESPIRATION, 2);
            data.enchantments.put(Enchantments.AQUA_AFFINITY, 1);
        }

        // 9. Невесомость (Светлое дерево / Бамбук)
        if (isArmor && counts[VoxelMaterial.LIGHT_WOOD.getId()] >= 35) {
            data.enchantments.put(Enchantments.FALL_PROTECTION, 2);
        }

        // 10. Починка (Осколок эха Вардена)
        if (counts[VoxelMaterial.ECHO_SHARD.getId()] >= 30) {
            data.enchantments.put(Enchantments.MENDING, 1);
        }

        // 11. Отдача (Глубинный сланец)
        if (!isArmor && counts[VoxelMaterial.DEEPSLATE.getId()] >= 30) {
            data.enchantments.put(Enchantments.KNOCKBACK, 2);
        }

        // 12. Небесная кара (Кость)
        if (!isArmor && counts[VoxelMaterial.BONE.getId()] >= 30) {
            data.enchantments.put(Enchantments.SMITE, 2);
        }
    }
}
