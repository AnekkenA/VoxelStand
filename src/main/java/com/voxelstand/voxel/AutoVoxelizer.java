package com.voxelstand.voxel;

import net.minecraft.world.item.*;

/**
 * Генератор начальной воксельной модели при первой установке ванильного предмета на стойку.
 * Создает детализированный, многотональный 3D воксельный каркас с градиентами,
 * акцентными контурами и аутентичной эстетикой Minecraft.
 */
public final class AutoVoxelizer {
    private AutoVoxelizer() {}

    /**
     * Определение основного воксельного материала по типу ванильного предмета.
     */
    public static VoxelMaterial detectBaseMaterial(ItemStack stack) {
        Item item = stack.getItem();
        String name = item.getDescriptionId().toLowerCase();

        if (name.contains("diamond")) return VoxelMaterial.DIAMOND;
        if (name.contains("netherite")) return VoxelMaterial.NETHERITE;
        if (name.contains("gold")) return VoxelMaterial.GOLD;
        if (name.contains("iron")) return VoxelMaterial.IRON;
        if (name.contains("copper")) return VoxelMaterial.COPPER;
        if (name.contains("leather")) return VoxelMaterial.LEATHER;
        if (name.contains("chainmail")) return VoxelMaterial.IRON;
        if (name.contains("stone")) return VoxelMaterial.DEEPSLATE;
        if (name.contains("wooden")) return VoxelMaterial.OAK;

        return VoxelMaterial.IRON;
    }

    /**
     * Палитра материалов для многотонального моделирования (база, лезвие/кромка, глубина, акцент).
     */
    private static class MaterialPalette {
        final byte base;
        final byte edge;
        final byte shadow;
        final byte accent;
        final byte grip;

        MaterialPalette(VoxelMaterial baseMat) {
            this.grip = VoxelMaterial.DARK_WOOD.getId();
            switch (baseMat) {
                case DEEPSLATE -> { // Камень
                    this.base = VoxelMaterial.DEEPSLATE.getId();
                    this.edge = VoxelMaterial.FLINT.getId();
                    this.shadow = VoxelMaterial.OBSIDIAN.getId();
                    this.accent = VoxelMaterial.QUARTZ.getId();
                }
                case IRON -> { // Железо
                    this.base = VoxelMaterial.IRON.getId();
                    this.edge = VoxelMaterial.QUARTZ.getId();
                    this.shadow = VoxelMaterial.FLINT.getId();
                    this.accent = VoxelMaterial.GOLD.getId();
                }
                case DIAMOND -> { // Алмаз
                    this.base = VoxelMaterial.DIAMOND.getId();
                    this.edge = VoxelMaterial.SEA_LANTERN.getId();
                    this.shadow = VoxelMaterial.DARK_PRISMARINE.getId();
                    this.accent = VoxelMaterial.LAPIS.getId();
                }
                case NETHERITE -> { // Незерит
                    this.base = VoxelMaterial.NETHERITE.getId();
                    this.edge = VoxelMaterial.DEEPSLATE.getId();
                    this.shadow = VoxelMaterial.OBSIDIAN.getId();
                    this.accent = VoxelMaterial.GOLD.getId();
                }
                case GOLD -> { // Золото
                    this.base = VoxelMaterial.GOLD.getId();
                    this.edge = VoxelMaterial.QUARTZ.getId();
                    this.shadow = VoxelMaterial.BLAZE.getId();
                    this.accent = VoxelMaterial.REDSTONE.getId();
                }
                case COPPER -> { // Медь
                    this.base = VoxelMaterial.COPPER.getId();
                    this.edge = VoxelMaterial.LIGHT_WOOD.getId();
                    this.shadow = VoxelMaterial.DARK_WOOD.getId();
                    this.accent = VoxelMaterial.WARPED.getId();
                }
                case LEATHER -> { // Кожа
                    this.base = VoxelMaterial.LEATHER.getId();
                    this.edge = VoxelMaterial.LIGHT_WOOD.getId();
                    this.shadow = VoxelMaterial.DARK_WOOD.getId();
                    this.accent = VoxelMaterial.IRON.getId();
                }
                default -> { // Дерево и др.
                    this.base = VoxelMaterial.OAK.getId();
                    this.edge = VoxelMaterial.LIGHT_WOOD.getId();
                    this.shadow = VoxelMaterial.DARK_WOOD.getId();
                    this.accent = VoxelMaterial.FLINT.getId();
                }
            }
        }
    }

    /**
     * Авто-вокселизация оружия (мечи, топоры).
     * Размещает рукоять строго в месте соломенного хвата (Y=6..12),
     * гарду над хватом (Y=12..14) и лезвие с выразительной двухцветной заточкой (Y=14..30).
     */
    public static void voxelizeWeapon(ItemStack stack, VoxelGrid grid) {
        grid.clear();
        MaterialPalette pal = new MaterialPalette(detectBaseMaterial(stack));

        // 1. Навершие рукояти (Pommel): Y=4..6, X=14..17, Z=14..17
        for (int x = 14; x <= 17; x++) {
            for (int y = 4; y <= 6; y++) {
                for (int z = 14; z <= 17; z++) {
                    grid.set(x, y, z, pal.base);
                }
            }
        }
        grid.set(15, 4, 15, pal.accent);
        grid.set(16, 4, 16, pal.accent);

        // 2. Рукоять (Hilt): Y=7..12, X=15..16, Z=15..16 (внутри соломенного хвата)
        for (int x = 15; x <= 16; x++) {
            for (int y = 7; y <= 12; y++) {
                for (int z = 15; z <= 16; z++) {
                    grid.set(x, y, z, (y % 2 == 0) ? pal.grip : pal.shadow);
                }
            }
        }

        // 3. Гарда / Крестовина (Crossguard): Y=13..14, размах по X от 9 до 22
        for (int x = 9; x <= 22; x++) {
            for (int y = 13; y <= 14; y++) {
                for (int z = 14; z <= 17; z++) {
                    byte mat = (x == 9 || x == 10 || x == 21 || x == 22) ? pal.accent : pal.base;
                    grid.set(x, y, z, mat);
                }
            }
        }

        // 4. Лезвие (Blade): Y=15..28, ширина X=14..17, толщина Z=15..16
        for (int y = 15; y <= 28; y++) {
            // Режущие кромки (слева X=14, справа X=17) — светлый заточенный материал
            for (int z = 15; z <= 16; z++) {
                grid.set(14, y, z, pal.edge);
                grid.set(17, y, z, pal.edge);
            }

            // Центральное ребро / дол (X=15..16) — базовый металл и теневая линия дола
            for (int z = 15; z <= 16; z++) {
                byte centerMat = (y >= 16 && y <= 24 && z == 15) ? pal.shadow : pal.base;
                grid.set(15, y, z, centerMat);
                grid.set(16, y, z, centerMat);
            }
        }

        // 5. Заостренный кончик клинка: Y=29..30
        for (int z = 15; z <= 16; z++) {
            grid.set(15, 29, z, pal.edge);
            grid.set(16, 29, z, pal.edge);
            grid.set(15, 30, z, pal.accent);
            grid.set(16, 30, z, pal.accent);
        }
    }

    /**
     * Авто-вокселизация шлема (Head grid).
     */
    public static void voxelizeHelmet(ItemStack stack, VoxelGrid headGrid) {
        headGrid.clear();
        MaterialPalette pal = new MaterialPalette(detectBaseMaterial(stack));

        // Купол шлема (сверху и по бокам головы 16x16)
        for (int x = 7; x <= 24; x++) {
            for (int z = 7; z <= 24; z++) {
                byte mat = (x == 15 || x == 16) ? pal.accent : pal.base;
                headGrid.set(x, 24, z, mat); // Макушка
                headGrid.set(x, 23, z, pal.base);
            }
        }

        // Затылок и виски
        for (int y = 12; y <= 22; y++) {
            for (int x = 7; x <= 24; x++) {
                byte edgeCheck = (y == 12 || x == 7 || x == 24) ? pal.edge : pal.base;
                headGrid.set(x, y, 7, edgeCheck); // Затылок
                headGrid.set(7, y, x, edgeCheck); // Левый висок
                headGrid.set(24, y, x, edgeCheck); // Правый висок
            }
        }

        // Лобная пластина и наносник над глазами
        for (int x = 8; x <= 23; x++) {
            headGrid.set(x, 18, 24, pal.edge); // Лоб
            headGrid.set(x, 17, 24, pal.base);
        }
        for (int y = 13; y <= 16; y++) {
            headGrid.set(15, y, 24, pal.accent);
            headGrid.set(16, y, 24, pal.accent);
        }
    }

    /**
     * Авто-вокселизация нагрудника (Body + LeftArm + RightArm).
     */
    public static void voxelizeChestplate(ItemStack stack, VoxelGrid bodyGrid, VoxelGrid leftArmGrid, VoxelGrid rightArmGrid) {
        bodyGrid.clear();
        leftArmGrid.clear();
        rightArmGrid.clear();
        MaterialPalette pal = new MaterialPalette(detectBaseMaterial(stack));

        // Кираса на торс: X=9..22, Y=8..24, Z=12..19
        for (int y = 8; y <= 24; y++) {
            for (int x = 9; x <= 22; x++) {
                byte bMat = (y == 8 || y == 24 || x == 9 || x == 22) ? pal.edge : pal.base;
                bodyGrid.set(x, y, 12, bMat); // Спина
                bodyGrid.set(x, y, 19, bMat); // Нагрудник (грудь)
            }
            for (int z = 12; z <= 19; z++) {
                bodyGrid.set(9, y, z, pal.shadow);  // Бок левый
                bodyGrid.set(22, y, z, pal.shadow); // Бок правый
            }
        }

        // Центральная эмблема / нагрудный гребень
        for (int y = 14; y <= 19; y++) {
            bodyGrid.set(15, y, 20, pal.accent);
            bodyGrid.set(16, y, 20, pal.accent);
        }

        // Наплечники на левой и правой руке
        for (int y = 18; y <= 24; y++) {
            for (int x = 11; x <= 20; x++) {
                byte aMat = (y == 24) ? pal.accent : pal.base;
                leftArmGrid.set(x, y, 11, aMat);
                leftArmGrid.set(x, y, 20, aMat);
                rightArmGrid.set(x, y, 11, aMat);
                rightArmGrid.set(x, y, 20, aMat);
            }
            for (int z = 11; z <= 20; z++) {
                leftArmGrid.set(11, y, z, pal.edge);
                leftArmGrid.set(20, y, z, pal.edge);
                rightArmGrid.set(11, y, z, pal.edge);
                rightArmGrid.set(20, y, z, pal.edge);
            }
        }
        for (int x = 11; x <= 20; x++) {
            for (int z = 11; z <= 20; z++) {
                leftArmGrid.set(x, 24, z, pal.accent);
                rightArmGrid.set(x, 24, z, pal.accent);
            }
        }
    }

    /**
     * Авто-вокселизация поножей.
     */
    public static void voxelizeLeggings(ItemStack stack, VoxelGrid pelvisGrid, VoxelGrid leftLegGrid, VoxelGrid rightLegGrid) {
        pelvisGrid.clear();
        leftLegGrid.clear();
        rightLegGrid.clear();
        MaterialPalette pal = new MaterialPalette(detectBaseMaterial(stack));

        // Пояс (Pelvis): Y=20..26, X=10..21, Z=12..19
        for (int y = 20; y <= 26; y++) {
            for (int x = 10; x <= 21; x++) {
                byte pMat = (y == 25 || y == 26) ? pal.edge : pal.base;
                pelvisGrid.set(x, y, 12, pMat);
                pelvisGrid.set(x, y, 19, pMat);
            }
        }
        // Пряжка ремня
        pelvisGrid.set(15, 23, 20, pal.accent);
        pelvisGrid.set(16, 23, 20, pal.accent);

        // Набедренники и наколенники
        for (int y = 10; y <= 24; y++) {
            for (int x = 11; x <= 20; x++) {
                byte legMat = (y >= 14 && y <= 16) ? pal.accent : pal.base; // Наколенник
                leftLegGrid.set(x, y, 19, legMat);
                rightLegGrid.set(x, y, 19, legMat);
            }
        }
    }

    /**
     * Авто-вокселизация ботинок.
     */
    public static void voxelizeBoots(ItemStack stack, VoxelGrid leftBootGrid, VoxelGrid rightBootGrid) {
        leftBootGrid.clear();
        rightBootGrid.clear();
        MaterialPalette pal = new MaterialPalette(detectBaseMaterial(stack));

        // Подошва и носок
        for (int x = 11; x <= 20; x++) {
            for (int z = 10; z <= 22; z++) {
                leftBootGrid.set(x, 1, z, pal.shadow);
                rightBootGrid.set(x, 1, z, pal.shadow);
            }
        }

        // Голенище
        for (int y = 2; y <= 10; y++) {
            for (int x = 11; x <= 20; x++) {
                byte bMat = (y == 10) ? pal.edge : pal.base;
                leftBootGrid.set(x, y, 11, bMat);
                leftBootGrid.set(x, y, 20, bMat);
                rightBootGrid.set(x, y, 11, bMat);
                rightBootGrid.set(x, y, 20, bMat);
            }
        }
    }

    public static void voxelizeShield(ItemStack stack, VoxelGrid grid) {
        grid.clear();
        MaterialPalette pal = new MaterialPalette(detectBaseMaterial(stack));
        for (int x = 6; x <= 25; x++) {
            for (int y = 4; y <= 27; y++) {
                byte sMat = (x == 6 || x == 25 || y == 4 || y == 27) ? pal.edge : pal.base;
                grid.set(x, y, 16, sMat);
            }
        }
        grid.set(15, 15, 17, pal.accent);
        grid.set(16, 15, 17, pal.accent);
    }

    public static void voxelizeBow(ItemStack stack, VoxelGrid grid) {
        grid.clear();
        MaterialPalette pal = new MaterialPalette(VoxelMaterial.OAK);
        for (int y = 4; y <= 28; y++) {
            grid.set(15, y, 15, pal.base);
            grid.set(16, y, 15, pal.base);
            grid.set(15, y, 17, pal.accent); // Тетива
        }
    }

    public static void voxelizeCrossbow(ItemStack stack, VoxelGrid grid) {
        grid.clear();
        MaterialPalette pal = new MaterialPalette(VoxelMaterial.DARK_WOOD);
        for (int z = 6; z <= 26; z++) {
            grid.set(15, 15, z, pal.base);
            grid.set(16, 15, z, pal.base);
        }
        for (int x = 8; x <= 23; x++) {
            grid.set(x, 15, 8, pal.edge);
        }
    }
}
