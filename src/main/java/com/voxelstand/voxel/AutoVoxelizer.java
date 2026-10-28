package com.voxelstand.voxel;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;

/**
 * Интеллектуальный генератор базового 3D-каркаса вокселей для любого предмета.
 * Поддерживает мечи, кирки, топоры, лопаты, мотыги, луки, арбалеты, щиты и все 4 типа брони.
 * Строго соблюдает аутентичность материалов:
 * - Алмазные инструменты состоят ТОЛЬКО из алмаза и дерева (без лазурита и чужеродных кристаллов!)
 * - Затенение выполняется через тональные вариации того же материала (светлый, базовый, теневой, глубокий).
 */
public class AutoVoxelizer {

    public static VoxelMaterial detectBaseMaterial(ItemStack stack) {
        String name = stack.getDescriptionId().toLowerCase();
        if (name.contains("netherite")) return VoxelMaterial.NETHERITE;
        if (name.contains("diamond")) return VoxelMaterial.DIAMOND;
        if (name.contains("gold")) return VoxelMaterial.GOLD;
        if (name.contains("iron")) return VoxelMaterial.IRON;
        if (name.contains("copper")) return VoxelMaterial.COPPER;
        if (name.contains("stone") || name.contains("chainmail")) return VoxelMaterial.DEEPSLATE;
        if (name.contains("leather")) return VoxelMaterial.LEATHER;
        return VoxelMaterial.IRON;
    }

    /**
     * Создание тонального ID материала:
     * tone: 0 = Базовый (1.0x), 1 = Светлый (+18%), 2 = Теневой (-18%), 3 = Глубокий (-32%)
     */
    private static byte toned(VoxelMaterial mat, int tone) {
        return (byte) (mat.getId() | ((tone & 3) << 5));
    }

    /**
     * Палитра материалов для многотонального моделирования.
     * Не подмешивает посторонние цветные материалы (лазурит к алмазу и т.д.).
     */
    private static class MaterialPalette {
        final byte base;
        final byte edge;
        final byte shadow;
        final byte accent;
        final byte grip;

        MaterialPalette(VoxelMaterial baseMat) {
            this.grip = toned(VoxelMaterial.DARK_WOOD, 0);
            this.base = toned(baseMat, 0);
            this.edge = toned(baseMat, 1);    // Светлый блик / заточка того же материала
            this.shadow = toned(baseMat, 2);  // Тень того же материала
            this.accent = toned(baseMat, 3);  // Глубокий рельеф того же материала
        }
    }

    /**
     * Авто-вокселизация оружия и инструментов (мечи, кирки, топоры, лопаты, мотыги).
     */
    public static void voxelizeWeapon(ItemStack stack, VoxelGrid grid) {
        grid.clear();
        MaterialPalette pal = new MaterialPalette(detectBaseMaterial(stack));

        if (stack.getItem() instanceof PickaxeItem) {
            voxelizePickaxe(grid, pal);
        } else if (stack.getItem() instanceof AxeItem) {
            voxelizeAxe(grid, pal);
        } else if (stack.getItem() instanceof ShovelItem) {
            voxelizeShovel(grid, pal);
        } else if (stack.getItem() instanceof HoeItem) {
            voxelizeHoe(grid, pal);
        } else {
            voxelizeSword(grid, pal);
        }
    }

    private static void voxelizeSword(VoxelGrid grid, MaterialPalette pal) {
        // 1. Навершие рукояти (Pommel): Y=3..5, X=14..17, Z=14..17
        for (int x = 14; x <= 17; x++) {
            for (int y = 3; y <= 5; y++) {
                for (int z = 14; z <= 17; z++) {
                    grid.set(x, y, z, pal.base);
                }
            }
        }
        grid.set(15, 3, 15, pal.accent);
        grid.set(16, 3, 16, pal.accent);

        // 2. Рукоять (Hilt): Y=6..12, X=15..16, Z=15..16 (вдоль соломенного хвата)
        for (int x = 15; x <= 16; x++) {
            for (int y = 6; y <= 12; y++) {
                for (int z = 15; z <= 16; z++) {
                    grid.set(x, y, z, (y % 2 == 0) ? pal.grip : toned(VoxelMaterial.OAK, 0));
                }
            }
        }

        // 3. Гарда / Крестовина (Crossguard): Y=13..14, размах по X от 9 до 22
        for (int x = 9; x <= 22; x++) {
            for (int y = 13; y <= 14; y++) {
                for (int z = 14; z <= 17; z++) {
                    byte mat = (x == 9 || x == 10 || x == 21 || x == 22) ? pal.edge : pal.base;
                    grid.set(x, y, z, mat);
                }
            }
        }

        // 4. Лезвие (Blade): Y=15..28, ширина X=14..17, толщина Z=15..16
        for (int y = 15; y <= 28; y++) {
            for (int z = 15; z <= 16; z++) {
                grid.set(14, y, z, pal.edge);
                grid.set(17, y, z, pal.edge);
                byte centerMat = (y >= 16 && y <= 24 && z == 15) ? pal.shadow : pal.base;
                grid.set(15, y, z, centerMat);
                grid.set(16, y, z, centerMat);
            }
        }

        // 5. Заостренный кончик клинка: Y=29..30
        for (int z = 15; z <= 16; z++) {
            grid.set(15, 29, z, pal.edge);
            grid.set(16, 29, z, pal.edge);
            grid.set(15, 30, z, pal.edge);
            grid.set(16, 30, z, pal.edge);
        }
    }

    private static void voxelizePickaxe(VoxelGrid grid, MaterialPalette pal) {
        // Длинная рукоять, опирающаяся на обе люльки: Y=2..25, X=15..16, Z=15..16
        for (int y = 2; y <= 25; y++) {
            for (int x = 15; x <= 16; x++) {
                for (int z = 15; z <= 16; z++) {
                    grid.set(x, y, z, (y % 3 == 0) ? pal.grip : toned(VoxelMaterial.OAK, 0));
                }
            }
        }
        // Навершие рукояти
        for (int x = 14; x <= 17; x++) {
            for (int z = 14; z <= 17; z++) {
                grid.set(x, 2, z, pal.base);
            }
        }

        // Центральная муфта кирки: Y=25..27, X=14..17, Z=14..17
        for (int x = 14; x <= 17; x++) {
            for (int y = 25; y <= 27; y++) {
                for (int z = 14; z <= 17; z++) {
                    grid.set(x, y, z, pal.base);
                }
            }
        }

        // Клювы кирки (дугообразные рога влево и вправо)
        // Левый рог: X=5..13
        for (int x = 5; x <= 13; x++) {
            int y = (x <= 7) ? 24 : 26;
            for (int z = 15; z <= 16; z++) {
                byte m = (x == 5) ? pal.edge : (z == 15 ? pal.edge : pal.base);
                grid.set(x, y, z, m);
                grid.set(x, y - 1, z, pal.shadow);
            }
        }

        // Правый рог: X=18..26
        for (int x = 18; x <= 26; x++) {
            int y = (x >= 24) ? 24 : 26;
            for (int z = 15; z <= 16; z++) {
                byte m = (x == 26) ? pal.edge : (z == 15 ? pal.edge : pal.base);
                grid.set(x, y, z, m);
                grid.set(x, y - 1, z, pal.shadow);
            }
        }
    }

    private static void voxelizeAxe(VoxelGrid grid, MaterialPalette pal) {
        // Рукоять: Y=2..25, X=15..16, Z=15..16
        for (int y = 2; y <= 25; y++) {
            for (int x = 15; x <= 16; x++) {
                for (int z = 15; z <= 16; z++) {
                    grid.set(x, y, z, (y % 3 == 0) ? pal.grip : toned(VoxelMaterial.OAK, 0));
                }
            }
        }
        // Навершие рукояти
        for (int x = 14; x <= 17; x++) {
            for (int z = 14; z <= 17; z++) {
                grid.set(x, 2, z, pal.base);
            }
        }

        // Обух топора: Y=22..27, X=13..17, Z=14..17
        for (int x = 13; x <= 17; x++) {
            for (int y = 22; y <= 27; y++) {
                for (int z = 14; z <= 17; z++) {
                    grid.set(x, y, z, pal.base);
                }
            }
        }

        // Лезвие топора (выступает вправо: X=18..26, Y=20..29)
        for (int x = 18; x <= 25; x++) {
            for (int y = 21; y <= 28; y++) {
                for (int z = 15; z <= 16; z++) {
                    grid.set(x, y, z, pal.base);
                }
            }
        }
        // Заточенная кромка
        for (int y = 20; y <= 29; y++) {
            for (int z = 15; z <= 16; z++) {
                grid.set(26, y, z, pal.edge);
            }
        }
    }

    private static void voxelizeShovel(VoxelGrid grid, MaterialPalette pal) {
        // Рукоять: Y=2..23, X=15..16, Z=15..16
        for (int y = 2; y <= 23; y++) {
            for (int x = 15; x <= 16; x++) {
                for (int z = 15; z <= 16; z++) {
                    grid.set(x, y, z, (y % 3 == 0) ? pal.grip : toned(VoxelMaterial.OAK, 0));
                }
            }
        }

        // Штык лопаты: Y=24..30, X=12..19, Z=15..16
        for (int x = 12; x <= 19; x++) {
            for (int y = 24; y <= 29; y++) {
                for (int z = 15; z <= 16; z++) {
                    byte m = (x == 12 || x == 19) ? pal.edge : pal.base;
                    grid.set(x, y, z, m);
                }
            }
        }
        // Заостренное острие лопаты
        for (int x = 14; x <= 17; x++) {
            for (int z = 15; z <= 16; z++) {
                grid.set(x, 30, z, pal.edge);
            }
        }
    }

    private static void voxelizeHoe(VoxelGrid grid, MaterialPalette pal) {
        // Рукоять: Y=2..25, X=15..16, Z=15..16
        for (int y = 2; y <= 25; y++) {
            for (int x = 15; x <= 16; x++) {
                for (int z = 15; z <= 16; z++) {
                    grid.set(x, y, z, (y % 3 == 0) ? pal.grip : toned(VoxelMaterial.OAK, 0));
                }
            }
        }

        // Лезвие мотыги: X=17..24, Y=25..27
        for (int x = 17; x <= 24; x++) {
            for (int y = 25; y <= 27; y++) {
                for (int z = 15; z <= 16; z++) {
                    grid.set(x, y, z, pal.base);
                }
            }
        }
        // Загнутый носик
        for (int y = 23; y <= 25; y++) {
            for (int z = 15; z <= 16; z++) {
                grid.set(24, y, z, pal.edge);
            }
        }
    }

    /**
     * Авто-вокселизация шлема (Head grid).
     * Y=16..31: идеально охватывает голову соломенного манекена.
     */
    public static void voxelizeHelmet(ItemStack stack, VoxelGrid headGrid) {
        headGrid.clear();
        MaterialPalette pal = new MaterialPalette(detectBaseMaterial(stack));

        // Купол шлема (сверху головы): Y=30..31, X=7..24, Z=7..24
        for (int x = 7; x <= 24; x++) {
            for (int z = 7; z <= 24; z++) {
                byte mat = (x == 15 || x == 16) ? pal.edge : pal.base;
                headGrid.set(x, 31, z, mat);
                headGrid.set(x, 30, z, pal.base);
            }
        }

        // Затылок и боковины: Y=16..29
        for (int y = 16; y <= 29; y++) {
            for (int x = 7; x <= 24; x++) {
                byte edgeCheck = (y == 16 || x == 7 || x == 24) ? pal.edge : pal.base;
                headGrid.set(x, y, 7, edgeCheck);  // Затылок
                headGrid.set(7, y, x, edgeCheck);  // Левый висок
                headGrid.set(24, y, x, edgeCheck); // Правый висок
            }
        }

        // Лобная пластина и наносник над глазами
        for (int x = 8; x <= 23; x++) {
            headGrid.set(x, 25, 24, pal.edge); // Лоб
            headGrid.set(x, 24, 24, pal.base);
        }
        for (int y = 19; y <= 23; y++) {
            headGrid.set(15, y, 24, pal.edge);
            headGrid.set(16, y, 24, pal.edge);
        }
    }

    /**
     * Авто-вокселизация нагрудника (Body + LeftArm + RightArm).
     * Y=8..24: состыковано с торсом манекена.
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
            bodyGrid.set(15, y, 20, pal.edge);
            bodyGrid.set(16, y, 20, pal.edge);
        }

        // Наплечники на левой и правой руке
        for (int y = 18; y <= 24; y++) {
            for (int x = 11; x <= 20; x++) {
                byte aMat = (y == 24) ? pal.edge : pal.base;
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
                leftArmGrid.set(x, 24, z, pal.edge);
                rightArmGrid.set(x, 24, z, pal.edge);
            }
        }
    }

    /**
     * Авто-вокселизация поножей.
     * Y=1..26: ноги доходят до столешницы (Y=1), пояс на Y=20..26.
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
        pelvisGrid.set(15, 23, 20, pal.edge);
        pelvisGrid.set(16, 23, 20, pal.edge);

        // Набедренники и поножи: идут от верха (Y=24) до низа стола (Y=1)
        for (int y = 1; y <= 24; y++) {
            for (int x = 11; x <= 20; x++) {
                byte legMat = (y >= 14 && y <= 16) ? pal.edge : pal.base; // Наколенник
                leftLegGrid.set(x, y, 19, legMat);
                rightLegGrid.set(x, y, 19, legMat);
                // Задняя часть бедра
                leftLegGrid.set(x, y, 12, pal.shadow);
                rightLegGrid.set(x, y, 12, pal.shadow);
            }
        }
    }

    /**
     * Авто-вокселизация ботинок.
     * Подошва на столешнице (Y=1), голенище Y=2..10.
     */
    public static void voxelizeBoots(ItemStack stack, VoxelGrid leftBootGrid, VoxelGrid rightBootGrid) {
        leftBootGrid.clear();
        rightBootGrid.clear();
        MaterialPalette pal = new MaterialPalette(detectBaseMaterial(stack));

        // Подошва и носок на уровне стола (Y=1)
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
        grid.set(15, 15, 17, pal.edge);
        grid.set(16, 15, 17, pal.edge);
    }

    public static void voxelizeBow(ItemStack stack, VoxelGrid grid) {
        grid.clear();
        MaterialPalette pal = new MaterialPalette(VoxelMaterial.OAK);
        for (int y = 4; y <= 28; y++) {
            grid.set(15, y, 15, pal.base);
            grid.set(16, y, 15, pal.base);
            grid.set(15, y, 17, pal.edge); // Тетива
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
