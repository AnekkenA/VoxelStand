package com.voxelstand.voxel;

import com.mojang.blaze3d.platform.NativeImage;
import com.voxelstand.block.entity.SculptStandBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

/**
 * Интеллектуальный транслятор 2D текстур предметов и текстурных разверток брони в 3D воксели.
 * - Для оружия и инструментов: считывает 2D спрайт из атласа и переносит каждый пиксель в воксели 2x2 толщиной 2 вокселя.
 * - Для брони: считывает развертку (UV map) брони (_layer_1.png и _layer_2.png) и формирует объемную оболочку.
 * - Сохраняет оригинальные материалы (алмаз, железо, дерево) и вычисляет тональное затенение по яркости пикселей.
 */
public class TextureVoxelizer {

    /**
     * Вокселизация оружия и инструментов по их 2D текстуре из атласа.
     * Возвращает true при успешной генерации.
     */
    public static boolean voxelizeWeaponFromTexture(ItemStack stack, VoxelGrid grid) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getItemRenderer() == null) return false;

        var bakedModel = mc.getItemRenderer().getModel(stack, null, null, 0);
        if (bakedModel == null) return false;

        TextureAtlasSprite sprite = bakedModel.getParticleIcon();
        if (sprite == null) return false;

        int width = sprite.contents().width();
        int height = sprite.contents().height();
        if (width <= 0 || height <= 0) return false;

        grid.clear();
        VoxelMaterial baseMat = AutoVoxelizer.detectBaseMaterial(stack);

        // 1. Предварительный расчет средней яркости непрозрачных пикселей лезвия
        long totalLuma = 0;
        int nonWoodCount = 0;

        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                int abgr = sprite.getPixelRGBA(0, px, py);
                int a = (abgr >> 24) & 0xFF;
                if (a < 30) continue;

                int r = abgr & 0xFF;
                int g = (abgr >> 8) & 0xFF;
                int b = (abgr >> 16) & 0xFF;

                if (!isWoodPixel(r, g, b)) {
                    totalLuma += (long) (0.299 * r + 0.587 * g + 0.114 * b);
                    nonWoodCount++;
                }
            }
        }

        double avgLuma = nonWoodCount > 0 ? (double) totalLuma / nonWoodCount : 128.0;

        // 2. Перенос пикселей в воксельную сетку 32x32x32
        // Спрайт обычно 16x16: масштаб 2 вокселя на пиксель
        double scaleX = 32.0 / width;
        double scaleY = 32.0 / height;

        int stepX = Math.max(1, (int) Math.round(scaleX));
        int stepY = Math.max(1, (int) Math.round(scaleY));

        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                int abgr = sprite.getPixelRGBA(0, px, py);
                int a = (abgr >> 24) & 0xFF;
                if (a < 30) continue;

                int r = abgr & 0xFF;
                int g = (abgr >> 8) & 0xFF;
                int b = (abgr >> 16) & 0xFF;
                double luma = 0.299 * r + 0.587 * g + 0.114 * b;

                byte materialId;
                if (isWoodPixel(r, g, b)) {
                    // Деревянный хват / рукоять
                    int tone = luma > 110 ? 1 : (luma < 60 ? 2 : 0);
                    materialId = (byte) (VoxelMaterial.DARK_WOOD.getId() | (tone << 5));
                } else {
                    // Материал клинка / обуха с тональным затенением
                    int tone;
                    if (luma > avgLuma * 1.15) {
                        tone = 1; // Блик / светлая кромка (+18%)
                    } else if (luma < avgLuma * 0.70) {
                        tone = 3; // Глубокая тень / контур (-32%)
                    } else if (luma < avgLuma * 0.88) {
                        tone = 2; // Тень (-18%)
                    } else {
                        tone = 0; // Базовый металл
                    }
                    materialId = (byte) (baseMat.getId() | (tone << 5));
                }

                // Маппинг координат: py=0 (верх текстуры) соответствует vy=30 (верх сетки)
                int startVx = (int) (px * scaleX);
                int startVy = (int) ((height - 1 - py) * scaleY);

                for (int dx = 0; dx < stepX; dx++) {
                    for (int dy = 0; dy < stepY; dy++) {
                        int vx = startVx + dx;
                        int vy = startVy + dy;
                        if (VoxelGrid.isInBounds(vx, vy, 15)) {
                            // Толщина лезвия 2 вокселя: Z=15 и Z=16
                            grid.set(vx, vy, 15, materialId);
                            grid.set(vx, vy, 16, materialId);
                        }
                    }
                }
            }
        }

        return grid.getNonEmptyCount() > 0;
    }

    /**
     * Вокселизация брони по её текстурной развертке (UV map _layer_1.png / _layer_2.png).
     */
    public static boolean voxelizeArmorFromTexture(ItemStack stack, SculptStandBlockEntity.StandMode mode, Map<String, VoxelGrid> grids) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return false;
        if (!(stack.getItem() instanceof ArmorItem armor)) return false;

        NativeImage image = loadArmorTextureImage(armor, mode);
        if (image == null) return false;

        VoxelMaterial baseMat = AutoVoxelizer.detectBaseMaterial(stack);
        int imgW = image.getWidth();
        int imgH = image.getHeight();

        try {
            switch (mode) {
                case HELMET -> {
                    VoxelGrid head = grids.computeIfAbsent("head", k -> new VoxelGrid());
                    head.clear();
                    buildBoxFromUV(image, imgW, imgH, head, baseMat,
                            8, 16, 8, 16, 16, 16, // x, y, z, w, h, d
                            8, 0, 16, 0, 8, 8, 24, 8, 16, 8, 0, 8, 8); // UVs
                    // Оверлей шлема (рога / забрало), если есть в текстуре (U+32)
                    if (imgW >= 64) {
                        buildBoxFromUV(image, imgW, imgH, head, baseMat,
                                7, 15, 7, 18, 18, 18,
                                40, 0, 48, 0, 40, 8, 56, 8, 48, 8, 32, 8, 8);
                    }
                }
                case CHESTPLATE -> {
                    VoxelGrid body = grids.computeIfAbsent("body", k -> new VoxelGrid());
                    VoxelGrid leftArm = grids.computeIfAbsent("left_arm", k -> new VoxelGrid());
                    VoxelGrid rightArm = grids.computeIfAbsent("right_arm", k -> new VoxelGrid());
                    body.clear();
                    leftArm.clear();
                    rightArm.clear();

                    // Торс: 16x24x8
                    buildBoxFromUV(image, imgW, imgH, body, baseMat,
                            8, 5, 12, 16, 24, 8,
                            20, 16, 28, 16, 20, 20, 32, 20, 28, 20, 16, 20, 4);

                    // Правая рука (наплечник): 8x24x8
                    buildBoxFromUV(image, imgW, imgH, rightArm, baseMat,
                            12, 5, 12, 8, 24, 8,
                            44, 16, 48, 16, 44, 20, 52, 20, 48, 20, 40, 20, 4);

                    // Левая рука: 8x24x8
                    buildBoxFromUV(image, imgW, imgH, leftArm, baseMat,
                            12, 5, 12, 8, 24, 8,
                            44, 16, 48, 16, 44, 20, 52, 20, 48, 20, 40, 20, 4);
                }
                case LEGGINGS -> {
                    VoxelGrid pelvis = grids.computeIfAbsent("pelvis", k -> new VoxelGrid());
                    VoxelGrid leftLeg = grids.computeIfAbsent("left_leg", k -> new VoxelGrid());
                    VoxelGrid rightLeg = grids.computeIfAbsent("right_leg", k -> new VoxelGrid());
                    pelvis.clear();
                    leftLeg.clear();
                    rightLeg.clear();

                    // Пояс: 16x8x8
                    buildBoxFromUV(image, imgW, imgH, pelvis, baseMat,
                            8, 18, 12, 16, 8, 8,
                            20, 16, 28, 16, 20, 20, 32, 20, 28, 20, 16, 20, 4);

                    // Поножи (левая и правая нога до стола): 8x20x8
                    buildBoxFromUV(image, imgW, imgH, rightLeg, baseMat,
                            12, 1, 12, 8, 20, 8,
                            4, 16, 8, 16, 4, 20, 12, 20, 8, 20, 0, 20, 4);
                    buildBoxFromUV(image, imgW, imgH, leftLeg, baseMat,
                            12, 1, 12, 8, 20, 8,
                            4, 16, 8, 16, 4, 20, 12, 20, 8, 20, 0, 20, 4);
                }
                case BOOTS -> {
                    VoxelGrid leftBoot = grids.computeIfAbsent("left_boot", k -> new VoxelGrid());
                    VoxelGrid rightBoot = grids.computeIfAbsent("right_boot", k -> new VoxelGrid());
                    leftBoot.clear();
                    rightBoot.clear();

                    // Сапоги на столешнице: 8x8x8
                    buildBoxFromUV(image, imgW, imgH, rightBoot, baseMat,
                            12, 1, 12, 8, 8, 8,
                            4, 26, 8, 26, 4, 28, 12, 28, 8, 28, 0, 28, 4);
                    buildBoxFromUV(image, imgW, imgH, leftBoot, baseMat,
                            12, 1, 12, 8, 8, 8,
                            4, 26, 8, 26, 4, 28, 12, 28, 8, 28, 0, 28, 4);
                }
                default -> {
                    return false;
                }
            }
            return true;
        } finally {
            image.close();
        }
    }

    /**
     * Построение объемной воксельной коробки (грани: перед, зад, бока, верх, низ) по координатам UV развертки.
     */
    private static void buildBoxFromUV(NativeImage img, int imgW, int imgH, VoxelGrid grid, VoxelMaterial baseMat,
                                       int x0, int y0, int z0, int w, int h, int d,
                                       int uTop, int vTop, int uBot, int vBot,
                                       int uFront, int vFront, int uBack, int vBack,
                                       int uLeft, int vLeft, int uRight, int vRight, int uvStepW) {

        double uScale = (double) imgW / 64.0;
        double vScale = (double) imgH / 32.0;

        // Передняя грань (Front: +Z)
        samplePlane(img, grid, baseMat, x0, y0, z0 + d - 1, w, h, 1, 0, 0, 1,
                (int) (uFront * uScale), (int) (vFront * vScale), (int) (w * 0.5 * uScale), (int) (h * 0.5 * vScale));

        // Задняя грань (Back: -Z)
        samplePlane(img, grid, baseMat, x0, y0, z0, w, h, 1, 0, 0, 1,
                (int) (uBack * uScale), (int) (vBack * vScale), (int) (w * 0.5 * uScale), (int) (h * 0.5 * vScale));

        // Правая грань (Right: +X)
        samplePlane(img, grid, baseMat, x0 + w - 1, y0, z0, d, h, 0, 1, 0, 1,
                (int) (uRight * uScale), (int) (vRight * vScale), (int) (d * 0.5 * uScale), (int) (h * 0.5 * vScale));

        // Левая грань (Left: -X)
        samplePlane(img, grid, baseMat, x0, y0, z0, d, h, 0, 1, 0, 1,
                (int) (uLeft * uScale), (int) (vLeft * vScale), (int) (d * 0.5 * uScale), (int) (h * 0.5 * vScale));

        // Верхняя грань (Top: +Y)
        samplePlane(img, grid, baseMat, x0, y0 + h - 1, z0, w, d, 1, 0, 0, 1,
                (int) (uTop * uScale), (int) (vTop * vScale), (int) (w * 0.5 * uScale), (int) (d * 0.5 * vScale));
    }

    private static void samplePlane(NativeImage img, VoxelGrid grid, VoxelMaterial baseMat,
                                    int ox, int oy, int oz, int extentU, int extentV,
                                    int dirU_x, int dirU_z, int dirV_x, int dirV_y,
                                    int srcU, int srcV, int srcW, int srcH) {
        if (srcW <= 0 || srcH <= 0) return;

        for (int v = 0; v < extentV; v++) {
            int py = srcV + (int) ((double) v / extentV * srcH);
            for (int u = 0; u < extentU; u++) {
                int px = srcU + (int) ((double) u / extentU * srcW);

                if (px < 0 || px >= img.getWidth() || py < 0 || py >= img.getHeight()) continue;

                int abgr = img.getPixelRGBA(px, py);
                int alpha = (abgr >> 24) & 0xFF;
                if (alpha < 30) continue; // Прозрачный пиксель (вырез забрала / горловины)

                int r = abgr & 0xFF;
                int g = (abgr >> 8) & 0xFF;
                int b = (abgr >> 16) & 0xFF;
                double luma = 0.299 * r + 0.587 * g + 0.114 * b;

                int tone = luma > 150 ? 1 : (luma < 70 ? 3 : (luma < 100 ? 2 : 0));
                byte val = (byte) (baseMat.getId() | (tone << 5));

                int vx = ox + u * dirU_x;
                int vy = oy + v * dirV_y;
                int vz = oz + u * dirU_z;

                if (VoxelGrid.isInBounds(vx, vy, vz)) {
                    grid.set(vx, vy, vz, val);
                }
            }
        }
    }

    private static boolean isWoodPixel(int r, int g, int b) {
        // Определение коричневого/древесного цвета рукояти
        return (r > 60 && g > 30 && b < 65 && r > b * 1.4 && g > b);
    }

    private static NativeImage loadArmorTextureImage(ArmorItem armor, SculptStandBlockEntity.StandMode mode) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return null;

        String matName = armor.getMaterial().getRegisteredName();
        if (matName.contains(":")) {
            matName = matName.substring(matName.indexOf(':') + 1);
        }
        String namespace = "minecraft";

        int layer = (mode == SculptStandBlockEntity.StandMode.LEGGINGS) ? 2 : 1;
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(namespace, "textures/models/armor/" + matName + "_layer_" + layer + ".png");

        try {
            Optional<Resource> res = mc.getResourceManager().getResource(loc);
            if (res.isPresent()) {
                try (InputStream stream = res.get().open()) {
                    return NativeImage.read(stream);
                }
            }
        } catch (Throwable ignored) {}

        return null;
    }
}
