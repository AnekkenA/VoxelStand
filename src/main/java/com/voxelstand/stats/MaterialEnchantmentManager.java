package com.voxelstand.stats;

import com.voxelstand.voxel.VoxelMaterial;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Менеджер назначения ванильных зачарований.
 * Зачарования даются ТОЛЬКО редкими/магическими инкрустациями (кварц, лазурит, ифрит, шалкер, аметист, эхо-осколок и т.д.).
 * Базовые конструкционные материалы (камень/сланец, железо, алмаз, дерево, кость) зачарований не дают!
 */
public final class MaterialEnchantmentManager {
    private MaterialEnchantmentManager() {}

    public static void applyEnchantments(int[] counts, ItemStatData data, ItemStack stack) {
        if (stack.getItem() instanceof BowItem) {
            // Лук: Пламя (Ифрит), Бесконечность (Лазурит), Сила (Кварц), Починка (Осколок эха)
            int blaze = counts[VoxelMaterial.BLAZE.getId()];
            if (blaze >= 15) data.enchantments.put(Enchantments.FLAME.location(), 1);

            int lapis = counts[VoxelMaterial.LAPIS.getId()];
            if (lapis >= 40) data.enchantments.put(Enchantments.INFINITY.location(), 1);

            int quartz = counts[VoxelMaterial.QUARTZ.getId()];
            int powerLvl = Math.min(5, quartz / 25);
            if (powerLvl > 0) data.enchantments.put(Enchantments.POWER.location(), powerLvl);

            if (counts[VoxelMaterial.ECHO_SHARD.getId()] >= 30) {
                data.enchantments.put(Enchantments.MENDING.location(), 1);
            }
            return;
        }

        if (stack.getItem() instanceof CrossbowItem) {
            // Арбалет: Пронзающая стрела (Шалкер), Быстрая перезарядка (Редстоун), Тройной выстрел (Кварц), Починка
            int shulker = counts[VoxelMaterial.SHULKER.getId()];
            int pierceLvl = Math.min(4, shulker / 25);
            if (pierceLvl > 0) data.enchantments.put(Enchantments.PIERCING.location(), pierceLvl);

            int redstone = counts[VoxelMaterial.REDSTONE.getId()];
            int quickLvl = Math.min(3, redstone / 25);
            if (quickLvl > 0) data.enchantments.put(Enchantments.QUICK_CHARGE.location(), quickLvl);

            int quartz = counts[VoxelMaterial.QUARTZ.getId()];
            if (quartz >= 40) data.enchantments.put(Enchantments.MULTISHOT.location(), 1);

            if (counts[VoxelMaterial.ECHO_SHARD.getId()] >= 30) {
                data.enchantments.put(Enchantments.MENDING.location(), 1);
            }
            return;
        }

        if (stack.getItem() instanceof ShieldItem) {
            // Щит: Шипы (Аметист), Прочность (Обсидиан), Починка (Осколок эха)
            int amethyst = counts[VoxelMaterial.AMETHYST.getId()];
            int thornsLvl = Math.min(3, amethyst / 20);
            if (thornsLvl > 0) data.enchantments.put(Enchantments.THORNS.location(), thornsLvl);

            if (counts[VoxelMaterial.OBSIDIAN.getId()] >= 35) {
                data.enchantments.put(Enchantments.UNBREAKING.location(), 3);
            }

            if (counts[VoxelMaterial.ECHO_SHARD.getId()] >= 30) {
                data.enchantments.put(Enchantments.MENDING.location(), 1);
            }
            return;
        }

        boolean isArmor = stack.getItem() instanceof ArmorItem;
        boolean isSword = stack.getItem() instanceof SwordItem;
        boolean isTool = stack.getItem() instanceof DiggerItem;

        applyEnchantments(counts, data, isArmor, isSword, isTool);
    }

    public static void applyEnchantments(int[] counts, ItemStatData data, boolean isArmor) {
        applyEnchantments(counts, data, isArmor, !isArmor, false);
    }

    public static void applyEnchantments(int[] counts, ItemStatData data, boolean isArmor, boolean isSword, boolean isTool) {
        // 1. Огонь (Стержень ифрита: BLAZE)
        int blaze = counts[VoxelMaterial.BLAZE.getId()];
        if (blaze >= 30) {
            if (isArmor) data.enchantments.put(Enchantments.FIRE_PROTECTION.location(), 2);
            else if (isSword) data.enchantments.put(Enchantments.FIRE_ASPECT.location(), 2);
        } else if (blaze >= 15) {
            if (isArmor) data.enchantments.put(Enchantments.FIRE_PROTECTION.location(), 1);
            else if (isSword) data.enchantments.put(Enchantments.FIRE_ASPECT.location(), 1);
        }

        // 2. Удача / Добыча (Лазурит: LAPIS)
        int lapis = counts[VoxelMaterial.LAPIS.getId()];
        if (lapis >= 60) {
            if (isSword) data.enchantments.put(Enchantments.LOOTING.location(), 3);
            else if (isTool) data.enchantments.put(Enchantments.FORTUNE.location(), 3);
        } else if (lapis >= 40) {
            if (isSword) data.enchantments.put(Enchantments.LOOTING.location(), 2);
            else if (isTool) data.enchantments.put(Enchantments.FORTUNE.location(), 2);
        } else if (lapis >= 20) {
            if (isSword) data.enchantments.put(Enchantments.LOOTING.location(), 1);
            else if (isTool) data.enchantments.put(Enchantments.FORTUNE.location(), 1);
        }

        // 3. Заточка / Эффективность (Кварц: QUARTZ)
        int quartz = counts[VoxelMaterial.QUARTZ.getId()];
        int quartzLevel = Math.min(5, quartz / 25);
        if (quartzLevel > 0) {
            if (isSword) {
                data.enchantments.put(Enchantments.SHARPNESS.location(), quartzLevel);
            } else if (isTool) {
                data.enchantments.put(Enchantments.EFFICIENCY.location(), quartzLevel);
            }
        }

        // 4. Взрывоустойчивость (Обсидиан: OBSIDIAN)
        if (isArmor && counts[VoxelMaterial.OBSIDIAN.getId()] >= 35) {
            data.enchantments.put(Enchantments.BLAST_PROTECTION.location(), 2);
        }

        // 5. Защита от снарядов / Шелковое касание (Шалкер: SHULKER)
        int shulker = counts[VoxelMaterial.SHULKER.getId()];
        if (shulker >= 25) {
            if (isArmor) data.enchantments.put(Enchantments.PROJECTILE_PROTECTION.location(), 2);
            else if (isTool) data.enchantments.put(Enchantments.SILK_TOUCH.location(), 1);
        }

        // 6. Шипы (Осколок аметиста: AMETHYST)
        if (isArmor) {
            int amethyst = counts[VoxelMaterial.AMETHYST.getId()];
            int thornsLvl = Math.min(3, amethyst / 20);
            if (thornsLvl > 0) {
                data.enchantments.put(Enchantments.THORNS.location(), thornsLvl);
            }
        }

        // 7. Подводное дыхание и Подводник (Морской фонарь: SEA_LANTERN)
        if (isArmor && counts[VoxelMaterial.SEA_LANTERN.getId()] >= 25) {
            data.enchantments.put(Enchantments.RESPIRATION.location(), 2);
            data.enchantments.put(Enchantments.AQUA_AFFINITY.location(), 1);
        }

        // 8. Починка (Осколок эха Вардена: ECHO_SHARD)
        if (counts[VoxelMaterial.ECHO_SHARD.getId()] >= 30) {
            data.enchantments.put(Enchantments.MENDING.location(), 1);
        }
    }
}
