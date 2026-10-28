package com.voxelstand.voxel;

import com.voxelstand.api.VoxelConstants;
import java.util.Arrays;

/**
 * 3D воксельная сетка размером 32x32x32.
 * Содержит 32768 ячеек, каждая хранит ID материала (0..31).
 */
public class VoxelGrid {
    public static final int SIZE = VoxelConstants.GRID_SIZE;
    private final byte[] voxels;
    private int nonEmptyCount;

    public VoxelGrid() {
        this.voxels = new byte[VoxelConstants.TOTAL_VOXELS];
        this.nonEmptyCount = 0;
    }

    public VoxelGrid(byte[] source) {
        this.voxels = new byte[VoxelConstants.TOTAL_VOXELS];
        System.arraycopy(source, 0, this.voxels, 0, Math.min(source.length, this.voxels.length));
        recalculateCount();
    }

    public static int index(int x, int y, int z) {
        return (x * SIZE + y) * SIZE + z;
    }

    public static boolean isInBounds(int x, int y, int z) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE && z >= 0 && z < SIZE;
    }

    public byte get(int x, int y, int z) {
        if (!isInBounds(x, y, z)) {
            return VoxelConstants.EMPTY_MATERIAL_ID;
        }
        return voxels[index(x, y, z)];
    }

    public VoxelMaterial getMaterial(int x, int y, int z) {
        return VoxelMaterial.byId(get(x, y, z));
    }

    public boolean set(int x, int y, int z, byte materialId) {
        if (!isInBounds(x, y, z)) {
            return false;
        }
        int idx = index(x, y, z);
        byte old = voxels[idx];
        if (old == materialId) {
            return false;
        }

        voxels[idx] = materialId;
        if (old == VoxelConstants.EMPTY_MATERIAL_ID && materialId != VoxelConstants.EMPTY_MATERIAL_ID) {
            nonEmptyCount++;
        } else if (old != VoxelConstants.EMPTY_MATERIAL_ID && materialId == VoxelConstants.EMPTY_MATERIAL_ID) {
            nonEmptyCount--;
        }
        return true;
    }

    public boolean set(int x, int y, int z, VoxelMaterial material) {
        return set(x, y, z, material.getId());
    }

    public boolean remove(int x, int y, int z) {
        return set(x, y, z, VoxelConstants.EMPTY_MATERIAL_ID);
    }

    public void clear() {
        Arrays.fill(voxels, VoxelConstants.EMPTY_MATERIAL_ID);
        nonEmptyCount = 0;
    }

    public int getNonEmptyCount() {
        return nonEmptyCount;
    }

    public void recalculateCount() {
        int count = 0;
        for (byte v : voxels) {
            if (v != VoxelConstants.EMPTY_MATERIAL_ID) {
                count++;
            }
        }
        this.nonEmptyCount = count;
    }

    public byte[] getRawData() {
        return voxels;
    }

    public VoxelGrid copy() {
        VoxelGrid copy = new VoxelGrid();
        System.arraycopy(this.voxels, 0, copy.voxels, 0, this.voxels.length);
        copy.nonEmptyCount = this.nonEmptyCount;
        return copy;
    }

    public void copyFrom(VoxelGrid other) {
        if (other == null) return;
        System.arraycopy(other.voxels, 0, this.voxels, 0, Math.min(other.voxels.length, this.voxels.length));
        this.nonEmptyCount = other.nonEmptyCount;
    }

    /**
     * Размещение пластины заданного размера (width x height) в плоскости.
     * Правило: заполняются ТОЛЬКО пустые воксели. Уже занятые воксели не перезаписываются!
     * 
     * @param originX Начальная координата X
     * @param originY Начальная координата Y
     * @param originZ Начальная координата Z
     * @param stepU Шаг по U (ось 1)
     * @param stepV Шаг по V (ось 2)
     * @param width Размер вдоль U
     * @param height Размер вдоль V
     * @param materialId ID материала
     * @return Количество фактически установленных новых вокселей
     */
    public int placePlate(int originX, int originY, int originZ,
                          int[] stepU, int[] stepV,
                          int width, int height, byte materialId) {
        int placed = 0;
        for (int u = 0; u < width; u++) {
            for (int v = 0; v < height; v++) {
                int px = originX + u * stepU[0] + v * stepV[0];
                int py = originY + u * stepU[1] + v * stepV[1];
                int pz = originZ + u * stepU[2] + v * stepV[2];

                if (isInBounds(px, py, pz)) {
                    // Ставим ТОЛЬКО если ячейка пуста!
                    if (get(px, py, pz) == VoxelConstants.EMPTY_MATERIAL_ID) {
                        set(px, py, pz, materialId);
                        placed++;
                    }
                }
            }
        }
        return placed;
    }
}
