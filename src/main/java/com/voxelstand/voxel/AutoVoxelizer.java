package com.voxelstand.voxel;

import net.minecraft.world.item.*;

/**
 * Генератор начальной воксельной модели при первой установке ванильного предмета на стойку.
 * Преобразует ванильный предмет (меч, кирасу, шлем и т.д.) в готовый базовый 3D воксельный каркас.
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
     * Авто-вокселизация оружия (мечи, топоры).
     * Размещает рукоять строго в месте соломенного хвата (Y=6..12),
     * гарду над хватом (Y=12..14) и лезвие (Y=14..30).
     */
    public static void voxelizeWeapon(ItemStack stack, VoxelGrid grid) {
        grid.clear();
        VoxelMaterial bladeMat = detectBaseMaterial(stack);
        VoxelMaterial hiltMat = VoxelMaterial.DARK_WOOD;
        VoxelMaterial guardMat = (bladeMat == VoxelMaterial.NETHERITE) ? VoxelMaterial.NETHERITE : VoxelMaterial.GOLD;

        byte bladeId = bladeMat.getId();
        byte hiltId = hiltMat.getId();
        byte guardId = guardMat.getId();

        // 1. Навершие рукояти (Pommel): Y=4..6, X=15..16, Z=15..16
        for (int x = 14; x <= 17; x++) {
            for (int y = 4; y <= 6; y++) {
                for (int z = 14; z <= 17; z++) {
                    grid.set(x, y, z, guardId);
                }
            }
        }

        // 2. Рукоять (Hilt): Y=7..12, X=15..16, Z=15..16 (внутри соломенного кулака)
        for (int x = 15; x <= 16; x++) {
            for (int y = 7; y <= 12; y++) {
                for (int z = 15; z <= 16; z++) {
                    grid.set(x, y, z, hiltId);
                }
            }
        }

        // 3. Гарда / Крестовина (Crossguard): Y=13..14, размах по X от 9 до 22
        for (int x = 9; x <= 22; x++) {
            for (int y = 13; y <= 14; y++) {
                for (int z = 14; z <= 17; z++) {
                    grid.set(x, y, z, guardId);
                }
            }
        }

        // 4. Лезвие (Blade): Y=15..30, ширина X=14..17, толщина Z=15..16
        for (int y = 15; y <= 28; y++) {
            for (int x = 14; x <= 17; x++) {
                for (int z = 15; z <= 16; z++) {
                    grid.set(x, y, z, bladeId);
                }
            }
        }

        // Заостренный кончик клинка: Y=29..30
        for (int z = 15; z <= 16; z++) {
            grid.set(15, 29, z, bladeId);
            grid.set(16, 29, z, bladeId);
            grid.set(15, 30, z, bladeId);
            grid.set(16, 30, z, bladeId);
        }
    }

    /**
     * Авто-вокселизация шлема (Head grid).
     */
    public static void voxelizeHelmet(ItemStack stack, VoxelGrid headGrid) {
        headGrid.clear();
        byte matId = detectBaseMaterial(stack).getId();

        // Купол шлема (сверху и по бокам головы 16x16)
        // Голова занимает X=8..23, Y=8..23, Z=8..23
        // Делаем купол толщиной в 1 воксель снаружи:
        for (int x = 7; x <= 24; x++) {
            for (int z = 7; z <= 24; z++) {
                headGrid.set(x, 24, z, matId); // Макушка
                headGrid.set(x, 23, z, matId);
            }
        }

        // Затылок и виски
        for (int y = 12; y <= 22; y++) {
            for (int x = 7; x <= 24; x++) {
                headGrid.set(x, y, 7, matId); // Затылок
                headGrid.set(7, y, x, matId); // Левый висок
                headGrid.set(24, y, x, matId); // Правый висок
            }
        }

        // Лобная пластина и наносник над глазами (глаза на Y=14..16)
        for (int x = 8; x <= 23; x++) {
            headGrid.set(x, 18, 24, matId); // Лоб
            headGrid.set(x, 17, 24, matId);
        }
        // Наносник между глазами (X=15..16, Y=14..16)
        for (int y = 13; y <= 16; y++) {
            headGrid.set(15, y, 24, matId);
            headGrid.set(16, y, 24, matId);
        }
    }

    /**
     * Авто-вокселизация нагрудника (Body + LeftArm + RightArm).
     */
    public static void voxelizeChestplate(ItemStack stack, VoxelGrid bodyGrid, VoxelGrid leftArmGrid, VoxelGrid rightArmGrid) {
        bodyGrid.clear();
        leftArmGrid.clear();
        rightArmGrid.clear();
        byte matId = detectBaseMaterial(stack).getId();

        // Кираса на торс: X=9..22, Y=8..24, Z=12..19
        for (int y = 8; y <= 24; y++) {
            for (int x = 9; x <= 22; x++) {
                bodyGrid.set(x, y, 12, matId); // Спина
                bodyGrid.set(x, y, 19, matId); // Нагрудник (грудь)
            }
            for (int z = 12; z <= 19; z++) {
                bodyGrid.set(9, y, z, matId);  // Бок левый
                bodyGrid.set(22, y, z, matId); // Бок правый
            }
        }

        // Наплечники на левой и правой руке (X=10..21, Y=18..24, Z=11..20)
        for (int y = 18; y <= 24; y++) {
            for (int x = 11; x <= 20; x++) {
                for (int z = 11; z <= 20; z++) {
                    leftArmGrid.set(x, y, z, matId);
                    rightArmGrid.set(x, y, z, matId);
                }
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
        byte matId = detectBaseMaterial(stack).getId();

        // Пояс (Pelvis): Y=20..26, X=10..21, Z=12..19
        for (int y = 20; y <= 26; y++) {
            for (int x = 10; x <= 21; x++) {
                pelvisGrid.set(x, y, 12, matId);
                pelvisGrid.set(x, y, 19, matId);
            }
        }

        // Набедренники и наколенники
        for (int y = 10; y <= 24; y++) {
            for (int x = 11; x <= 20; x++) {
                leftLegGrid.set(x, y, 19, matId);
                rightLegGrid.set(x, y, 19, matId);
            }
        }
    }

    /**
     * Авто-вокселизация ботинок.
     */
    public static void voxelizeBoots(ItemStack stack, VoxelGrid leftBootGrid, VoxelGrid rightBootGrid) {
        leftBootGrid.clear();
        rightBootGrid.clear();
        byte matId = detectBaseMaterial(stack).getId();

        // Ступни и голенище: Y=0..10
        for (int y = 0; y <= 8; y++) {
            for (int x = 10; x <= 21; x++) {
                for (int z = 9; z <= 22; z++) {
                    leftBootGrid.set(x, y, z, matId);
                    rightBootGrid.set(x, y, z, matId);
                }
            }
        }
    }

    /**
     * Авто-вокселизация щита.
     * Деревянный каркас с металлической окантовкой и центральным умбоном.
     */
    public static void voxelizeShield(ItemStack stack, VoxelGrid grid) {
        grid.clear();
        byte woodId = VoxelMaterial.OAK.getId();
        byte metalId = detectBaseMaterial(stack).getId();
        if (metalId == VoxelMaterial.OAK.getId()) {
            metalId = VoxelMaterial.IRON.getId();
        }

        // 1. Деревянное поле щита: X=9..22, Y=6..25, Z=15..16
        for (int x = 9; x <= 22; x++) {
            for (int y = 6; y <= 25; y++) {
                grid.set(x, y, 15, woodId);
                grid.set(x, y, 16, woodId);
            }
        }

        // 2. Металлическая окантовка щита
        for (int y = 5; y <= 26; y++) {
            grid.set(8, y, 16, metalId);
            grid.set(23, y, 16, metalId);
        }
        for (int x = 8; x <= 23; x++) {
            grid.set(x, 26, 16, metalId);
            grid.set(x, 5, 16, metalId);
        }

        // 3. Металлический умбон по центру: X=14..17, Y=14..17, Z=17
        for (int x = 14; x <= 17; x++) {
            for (int y = 14; y <= 17; y++) {
                grid.set(x, y, 17, metalId);
            }
        }
    }

    /**
     * Авто-вокселизация лука.
     * Изогнутая деревянная дуга с белой тетивой.
     */
    public static void voxelizeBow(ItemStack stack, VoxelGrid grid) {
        grid.clear();
        byte woodId = detectBaseMaterial(stack).getId();
        byte stringId = VoxelMaterial.QUARTZ.getId();

        // 1. Центральный хват: X=15..16, Y=14..17, Z=16
        for (int y = 14; y <= 17; y++) {
            grid.set(15, y, 16, woodId);
            grid.set(16, y, 16, woodId);
        }

        // 2. Верхнее плечо дуги
        for (int y = 18; y <= 22; y++) {
            grid.set(15, y, 15, woodId);
            grid.set(16, y, 15, woodId);
        }
        for (int y = 23; y <= 27; y++) {
            grid.set(15, y, 14, woodId);
            grid.set(16, y, 14, woodId);
        }

        // 3. Нижнее плечо дуги
        for (int y = 9; y <= 13; y++) {
            grid.set(15, y, 15, woodId);
            grid.set(16, y, 15, woodId);
        }
        for (int y = 4; y <= 8; y++) {
            grid.set(15, y, 14, woodId);
            grid.set(16, y, 14, woodId);
        }

        // 4. Тетива (прямая линия от верхнего до нижнего наконечника по Z=17)
        for (int y = 4; y <= 27; y++) {
            grid.set(15, y, 17, stringId);
        }
    }

    /**
     * Авто-вокселизация арбалета.
     * Деревянное ложе, поперечная дуга и натянутая тетива.
     */
    public static void voxelizeCrossbow(ItemStack stack, VoxelGrid grid) {
        grid.clear();
        byte woodId = VoxelMaterial.DARK_WOOD.getId();
        byte ironId = VoxelMaterial.IRON.getId();
        byte stringId = VoxelMaterial.QUARTZ.getId();

        // 1. Ложе (приклад и ствол): Z=8..25, X=15..16, Y=15..16
        for (int z = 8; z <= 25; z++) {
            grid.set(15, 15, z, woodId);
            grid.set(16, 15, z, woodId);
            grid.set(15, 16, z, woodId);
            grid.set(16, 16, z, woodId);
        }

        // 2. Поперечная дуга (Prod): Z=24, X=7..24, Y=15..16
        for (int x = 7; x <= 24; x++) {
            grid.set(x, 15, 24, ironId);
            grid.set(x, 16, 24, ironId);
        }

        // 3. Передняя скоба (Стремя): Z=26, X=13..18
        for (int x = 13; x <= 18; x++) {
            grid.set(x, 15, 26, ironId);
        }

        // 4. Тетива арбалета: X=7..24, Z=20, Y=16
        for (int x = 7; x <= 24; x++) {
            grid.set(x, 16, 20, stringId);
        }
    }
}
