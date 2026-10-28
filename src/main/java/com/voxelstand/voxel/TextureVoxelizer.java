package com.voxelstand.voxel;

import com.mojang.blaze3d.platform.NativeImage;
import com.voxelstand.api.VoxelConstants;
import com.voxelstand.api.VoxelDataHelper;
import com.voxelstand.block.entity.SculptStandBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Интеллектуальный транслятор 2D текстур предметов и текстурных разверток брони в 3D воксели.
 * - Для оружия и инструментов: считывает 2D спрайт из атласа и переносит каждый пиксель в воксели 2x2 толщиной 2 вокселя.
 * - Для брони: считывает развертку (UV map) брони (_layer_1.png и _layer_2.png) и формирует объемную оболочку.
 * - Сохраняет оригинальные материалы (алмаз, железо, дерево) и динамически вычисляет тональное затенение по яркости пикселей.
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

        // 1. Первый проход: вычисление диапазонов яркости (minLuma, maxLuma) отдельно для лезвия и рукояти
        double bladeMin = 255.0, bladeMax = 0.0;
        double woodMin = 255.0, woodMax = 0.0;
        int bladeCount = 0;
        int woodCount = 0;

        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                int abgr = sprite.getPixelRGBA(0, px, py);
                int a = (abgr >> 24) & 0xFF;
                if (a < 30) continue;

                int r = abgr & 0xFF;
                int g = (abgr >> 8) & 0xFF;
                int b = (abgr >> 16) & 0xFF;
                double luma = 0.299 * r + 0.587 * g + 0.114 * b;

                if (isWoodPixel(r, g, b) && baseMat != VoxelMaterial.OAK) {
                    woodMin = Math.min(woodMin, luma);
                    woodMax = Math.max(woodMax, luma);
                    woodCount++;
                } else {
                    bladeMin = Math.min(bladeMin, luma);
                    bladeMax = Math.max(bladeMax, luma);
                    bladeCount++;
                }
            }
        }

        // 2. Второй проход: маппинг пикселей с тональным затенением
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
                if (isWoodPixel(r, g, b) && baseMat != VoxelMaterial.OAK) {
                    // Деревянный хват / рукоять инструмента
                    int tone;
                    if (woodMax > woodMin) {
                        double t = (luma - woodMin) / (woodMax - woodMin);
                        if (t >= 0.65) tone = 1;
                        else if (t >= 0.30) tone = 0;
                        else tone = 2;
                    } else {
                        tone = 0;
                    }
                    materialId = (byte) (VoxelMaterial.DARK_WOOD.getId() | (tone << 5));
                } else {
                    // Материал лезвия / обуха с адаптивным квантильным затенением
                    int tone;
                    if (bladeMax > bladeMin) {
                        double t = (luma - bladeMin) / (bladeMax - bladeMin);
                        if (t >= 0.72) {
                            tone = 1; // Светлый блик / острая кромка (+18%)
                        } else if (t >= 0.45) {
                            tone = 0; // Базовый тон металла / камня (1.0x)
                        } else if (t >= 0.22) {
                            tone = 2; // Тень (-18%)
                        } else {
                            tone = 3; // Глубокая тень / контур (-32%)
                        }
                    } else {
                        tone = 0;
                    }
                    materialId = (byte) (baseMat.getId() | (tone << 5));
                }

                int startVx = (int) (px * scaleX);
                int startVy = (int) ((height - 1 - py) * scaleY);

                for (int dx = 0; dx < stepX; dx++) {
                    for (int dy = 0; dy < stepY; dy++) {
                        int vx = startVx + dx;
                        int vy = startVy + dy;
                        if (VoxelGrid.isInBounds(vx, vy, 15)) {
                            // Толщина клинка 2 вокселя: Z=15 и Z=16
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

        // Расчет диапазона яркости для адаптивного затенения брони
        double minLuma = 255.0, maxLuma = 0.0;
        for (int y = 0; y < imgH; y++) {
            for (int x = 0; x < imgW; x++) {
                int abgr = image.getPixelRGBA(x, y);
                int a = (abgr >> 24) & 0xFF;
                if (a < 30) continue;
                int r = abgr & 0xFF;
                int g = (abgr >> 8) & 0xFF;
                int b = (abgr >> 16) & 0xFF;
                double luma = 0.299 * r + 0.587 * g + 0.114 * b;
                minLuma = Math.min(minLuma, luma);
                maxLuma = Math.max(maxLuma, luma);
            }
        }

        try {
            switch (mode) {
                case HELMET -> {
                    VoxelGrid head = grids.computeIfAbsent("head", k -> new VoxelGrid());
                    head.clear();
                    // Шлем: базовый куб 16x16x16 (X=8..24, Y=16..32, Z=8..24)
                    buildArmorBox(image, imgW, imgH, head, baseMat,
                            8, 16, 8, 16, 16, 16,
                            0, 0, 8, 8, 8, minLuma, maxLuma);

                    // Оверлей шлема (второй слой UV U=32): 18x18x18 (X=7..25, Y=15..33, Z=7..25)
                    if (imgW >= 64) {
                        buildArmorBox(image, imgW, imgH, head, baseMat,
                                7, 15, 7, 18, 18, 18,
                                32, 0, 8, 8, 8, minLuma, maxLuma);
                    }
                }
                case CHESTPLATE -> {
                    VoxelGrid body = grids.computeIfAbsent("body", k -> new VoxelGrid());
                    VoxelGrid leftArm = grids.computeIfAbsent("left_arm", k -> new VoxelGrid());
                    VoxelGrid rightArm = grids.computeIfAbsent("right_arm", k -> new VoxelGrid());
                    body.clear();
                    leftArm.clear();
                    rightArm.clear();

                    // Торс: 16x24x8 (X=8..24, Y=4..28, Z=12..20)
                    buildArmorBox(image, imgW, imgH, body, baseMat,
                            8, 4, 12, 16, 24, 8,
                            16, 16, 8, 12, 4, minLuma, maxLuma);

                    // Правый наплечник / рука: 8x24x8 (X=12..20, Y=4..28, Z=12..20)
                    buildArmorBox(image, imgW, imgH, rightArm, baseMat,
                            12, 4, 12, 8, 24, 8,
                            40, 16, 4, 12, 4, minLuma, maxLuma);

                    // Левый наплечник / рука: 8x24x8
                    buildArmorBox(image, imgW, imgH, leftArm, baseMat,
                            12, 4, 12, 8, 24, 8,
                            40, 16, 4, 12, 4, minLuma, maxLuma);
                }
                case LEGGINGS -> {
                    VoxelGrid pelvis = grids.computeIfAbsent("pelvis", k -> new VoxelGrid());
                    VoxelGrid leftLeg = grids.computeIfAbsent("left_leg", k -> new VoxelGrid());
                    VoxelGrid rightLeg = grids.computeIfAbsent("right_leg", k -> new VoxelGrid());
                    pelvis.clear();
                    leftLeg.clear();
                    rightLeg.clear();

                    // Пояс: 16x8x8 (X=8..24, Y=16..24, Z=12..20)
                    buildArmorBox(image, imgW, imgH, pelvis, baseMat,
                            8, 16, 12, 16, 8, 8,
                            16, 16, 8, 4, 4, minLuma, maxLuma);

                    // Поножи (правая нога): 8x16x8 (X=12..20, Y=0..16, Z=12..20)
                    buildArmorBox(image, imgW, imgH, rightLeg, baseMat,
                            12, 0, 12, 8, 16, 8,
                            0, 16, 4, 8, 4, minLuma, maxLuma);

                    // Поножи (левая нога): 8x16x8
                    buildArmorBox(image, imgW, imgH, leftLeg, baseMat,
                            12, 0, 12, 8, 16, 8,
                            0, 16, 4, 8, 4, minLuma, maxLuma);
                }
                case BOOTS -> {
                    VoxelGrid leftBoot = grids.computeIfAbsent("left_boot", k -> new VoxelGrid());
                    VoxelGrid rightBoot = grids.computeIfAbsent("right_boot", k -> new VoxelGrid());
                    leftBoot.clear();
                    rightBoot.clear();

                    // Сапоги на столешнице: 8x8x8 (X=12..20, Y=0..8, Z=12..20)
                    buildArmorBox(image, imgW, imgH, rightBoot, baseMat,
                            12, 0, 12, 8, 8, 8,
                            0, 24, 4, 4, 4, minLuma, maxLuma);

                    buildArmorBox(image, imgW, imgH, leftBoot, baseMat,
                            12, 0, 12, 8, 8, 8,
                            0, 24, 4, 4, 4, minLuma, maxLuma);
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
     * Построение объемной воксельной коробки брони по стандартной UV-развертке Minecraft.
     */
    private static void buildArmorBox(NativeImage img, int imgW, int imgH, VoxelGrid grid, VoxelMaterial baseMat,
                                      int x0, int y0, int z0, int w, int h, int d,
                                      int u0, int v0, int texW, int texH, int texD,
                                      double minLuma, double maxLuma) {
        double uScale = (double) imgW / 64.0;
        double vScale = (double) imgH / 32.0;

        // 1. Передняя грань (Front: +Z)
        sampleFace(img, grid, baseMat,
                (int) ((u0 + texD) * uScale), (int) ((v0 + texD) * vScale),
                (int) (texW * uScale), (int) (texH * vScale),
                x0, y0, z0 + d - 1, w, h, 1,
                FaceType.FRONT, minLuma, maxLuma);

        // 2. Задняя грань (Back: -Z)
        sampleFace(img, grid, baseMat,
                (int) ((u0 + 2 * texD + texW) * uScale), (int) ((v0 + texD) * vScale),
                (int) (texW * uScale), (int) (texH * vScale),
                x0, y0, z0, w, h, 1,
                FaceType.BACK, minLuma, maxLuma);

        // 3. Правая грань (Right: -X)
        sampleFace(img, grid, baseMat,
                (int) (u0 * uScale), (int) ((v0 + texD) * vScale),
                (int) (texD * uScale), (int) (texH * vScale),
                x0, y0, z0, d, h, 1,
                FaceType.RIGHT, minLuma, maxLuma);

        // 4. Левая грань (Left: +X)
        sampleFace(img, grid, baseMat,
                (int) ((u0 + texD + texW) * uScale), (int) ((v0 + texD) * vScale),
                (int) (texD * uScale), (int) (texH * vScale),
                x0 + w - 1, y0, z0, d, h, 1,
                FaceType.LEFT, minLuma, maxLuma);

        // 5. Верхняя грань (Top: +Y)
        sampleFace(img, grid, baseMat,
                (int) ((u0 + texD) * uScale), (int) (v0 * vScale),
                (int) (texW * uScale), (int) (texD * vScale),
                x0, y0 + h - 1, z0, w, d, 1,
                FaceType.TOP, minLuma, maxLuma);

        // 6. Нижняя грань (Bottom: -Y)
        sampleFace(img, grid, baseMat,
                (int) ((u0 + texD + texW) * uScale), (int) (v0 * vScale),
                (int) (texW * uScale), (int) (texD * vScale),
                x0, y0, z0, w, d, 1,
                FaceType.BOTTOM, minLuma, maxLuma);
    }

    private enum FaceType {
        FRONT, BACK, RIGHT, LEFT, TOP, BOTTOM
    }

    private static void sampleFace(NativeImage img, VoxelGrid grid, VoxelMaterial baseMat,
                                   int srcU, int srcV, int srcW, int srcH,
                                   int ox, int oy, int oz, int extentU, int extentV, int thickness,
                                   FaceType face, double minLuma, double maxLuma) {
        if (srcW <= 0 || srcH <= 0) return;

        for (int v = 0; v < extentV; v++) {
            int py = srcV + (int) ((double) v / extentV * srcH);
            for (int u = 0; u < extentU; u++) {
                int px = srcU + (int) ((double) u / extentU * srcW);

                if (px < 0 || px >= img.getWidth() || py < 0 || py >= img.getHeight()) continue;

                int abgr = img.getPixelRGBA(px, py);
                int alpha = (abgr >> 24) & 0xFF;
                if (alpha < 30) continue; // Прозрачный вырез

                int r = abgr & 0xFF;
                int g = (abgr >> 8) & 0xFF;
                int b = (abgr >> 16) & 0xFF;
                double luma = 0.299 * r + 0.587 * g + 0.114 * b;

                int tone;
                if (maxLuma > minLuma) {
                    double t = (luma - minLuma) / (maxLuma - minLuma);
                    if (t >= 0.72) tone = 1;      // Светлый блик (+18%)
                    else if (t >= 0.45) tone = 0; // Базовый металл
                    else if (t >= 0.22) tone = 2; // Тень (-18%)
                    else tone = 3;                // Глубокий контур (-32%)
                } else {
                    tone = 0;
                }
                byte val = (byte) (baseMat.getId() | (tone << 5));

                int vx, vy, vz;
                // В текстуре v=0 - это ВЕРХ, v=extentV-1 - это НИЗ
                int invV = (extentV - 1 - v);

                switch (face) {
                    case FRONT -> {
                        vx = ox + u;
                        vy = oy + invV;
                        vz = oz;
                    }
                    case BACK -> {
                        vx = ox + (extentU - 1 - u);
                        vy = oy + invV;
                        vz = oz;
                    }
                    case RIGHT -> {
                        vx = ox;
                        vy = oy + invV;
                        vz = oz + u;
                    }
                    case LEFT -> {
                        vx = ox;
                        vy = oy + invV;
                        vz = oz + (extentU - 1 - u);
                    }
                    case TOP -> {
                        vx = ox + u;
                        vy = oy;
                        vz = oz + invV;
                    }
                    case BOTTOM -> {
                        vx = ox + u;
                        vy = oy;
                        vz = oz + v;
                    }
                    default -> {
                        vx = ox; vy = oy; vz = oz;
                    }
                }

                if (VoxelGrid.isInBounds(vx, vy, vz)) {
                    grid.set(vx, vy, vz, val);
                }
            }
        }
    }

    /**
     * Надежный тест цвета на деревянную рукоять / палку.
     * Древесина в Minecraft имеет золотисто-коричневые тона (r > g > b, выраженный теплый градиент).
     * Металлы и камни нейтральны (r ≈ g ≈ b).
     */
    private static boolean isWoodPixel(int r, int g, int b) {
        return (r > 50 && r <= 190 && g > 25 && g <= 145 && r > g && g >= b && (r - b) >= 16 && ((r - b) / (float) r) >= 0.18f);
    }

    /**
     * Автоматическая вокселизация элемента брони, если модель еще не создана.
     */
    public static boolean ensureArmorVoxelized(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem)) return false;
        if (VoxelDataHelper.hasCustomKey(stack, VoxelConstants.NBT_VOXEL_MODEL)) {
            return true;
        }

        SculptStandBlockEntity.StandMode mode = switch (slot) {
            case HEAD -> SculptStandBlockEntity.StandMode.HELMET;
            case CHEST -> SculptStandBlockEntity.StandMode.CHESTPLATE;
            case LEGS -> SculptStandBlockEntity.StandMode.LEGGINGS;
            case FEET -> SculptStandBlockEntity.StandMode.BOOTS;
            default -> null;
        };
        if (mode == null) return false;

        Map<String, VoxelGrid> grids = new HashMap<>();
        if (voxelizeArmorFromTexture(stack, mode, grids)) {
            CompoundTag modelTag = new CompoundTag();
            byte[] mainData = null;
            for (Map.Entry<String, VoxelGrid> entry : grids.entrySet()) {
                byte[] compressed = VoxelCompressor.compress(entry.getValue());
                modelTag.putByteArray(entry.getKey(), compressed);
                if (mainData == null) mainData = compressed;
            }
            CompoundTag rootTag = VoxelDataHelper.getOrCreateCustomData(stack);
            rootTag.put(VoxelConstants.NBT_VOXEL_MODEL, modelTag);
            if (mainData != null) {
                rootTag.putByteArray(VoxelConstants.NBT_VOXEL_DATA, mainData);
            }
            VoxelDataHelper.setCustomData(stack, rootTag);
            return true;
        }
        return false;
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
