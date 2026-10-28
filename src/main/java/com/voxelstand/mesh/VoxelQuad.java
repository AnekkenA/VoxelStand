package com.voxelstand.mesh;

import com.voxelstand.voxel.VoxelMaterial;

/**
 * Оптимизированный прямоугольный полигон (Quad), созданный жадным мешингом.
 * Поддерживает 5-битную палитру материалов (0..31) и 2-битное тональное затенение игрока (биты 5..6):
 *  0 = Базовый тон (1.0x)
 *  1 = Светлый тон (+18% яркости)
 *  2 = Тёмный тон / тень (-18% яркости)
 *  3 = Глубокая тень (-32% яркости)
 */
public class VoxelQuad {
    public final float x1, y1, z1;
    public final float x2, y2, z2;
    public final VoxelFaceDirection direction;
    public final byte materialId;
    public final boolean emissive;
    public final int rgb;

    public VoxelQuad(float x1, float y1, float z1,
                     float x2, float y2, float z2,
                     VoxelFaceDirection direction, byte materialId) {
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
        this.direction = direction;
        this.materialId = materialId;

        VoxelMaterial mat = VoxelMaterial.byId(materialId);
        this.emissive = mat.isEmissive();
        int baseRgb = mat.getRgb();

        // Извлечение тона из бит 5 и 6 (0..3)
        int tone = ((materialId & 0xFF) >> 5) & 3;
        float factor = switch (tone) {
            case 1 -> 1.18f; // Светлее
            case 2 -> 0.82f; // Теневой
            case 3 -> 0.68f; // Глубокая тень
            default -> 1.0f; // Базовый
        };

        int r = Math.min(255, Math.max(0, (int) (((baseRgb >> 16) & 0xFF) * factor)));
        int g = Math.min(255, Math.max(0, (int) (((baseRgb >> 8) & 0xFF) * factor)));
        int b = Math.min(255, Math.max(0, (int) ((baseRgb & 0xFF) * factor)));
        this.rgb = (r << 16) | (g << 8) | b;
    }

    public float getRed() {
        return ((rgb >> 16) & 0xFF) / 255.0f;
    }

    public float getGreen() {
        return ((rgb >> 8) & 0xFF) / 255.0f;
    }

    public float getBlue() {
        return (rgb & 0xFF) / 255.0f;
    }
}
