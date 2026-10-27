package com.voxelstand.mesh;

import com.voxelstand.voxel.VoxelMaterial;

/**
 * Оптимизированный прямоугольный полигон (Quad), созданный жадным мешингом.
 * Координаты нормализованы в диапазоне от 0.0 до 1.0 (или от 0 до 32).
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
        this.rgb = mat.getRgb();
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
