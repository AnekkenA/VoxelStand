package com.voxelstand.voxel;

import com.voxelstand.api.VoxelConstants;

/**
 * Утилиты для расчета зеркальной симметрии вокселей.
 * Центральная плоскость симметрии X делит сетку 32x32x32 между индексами 15 и 16.
 */
public final class SymmetryHelper {
    private SymmetryHelper() {}

    /**
     * Вычисляет зеркальную координату X относительно центра (между 15 и 16).
     * Для x = 0 зеркало 31; для x = 15 зеркало 16.
     */
    public static int mirrorX(int x) {
        return (VoxelConstants.GRID_SIZE - 1) - x;
    }

    /**
     * Вычисляет зеркальную координату Y.
     */
    public static int mirrorY(int y) {
        return (VoxelConstants.GRID_SIZE - 1) - y;
    }

    /**
     * Вычисляет зеркальную координату Z.
     */
    public static int mirrorZ(int z) {
        return (VoxelConstants.GRID_SIZE - 1) - z;
    }

    /**
     * Зеркальное удаление вокселя резцом (долотом).
     * Удаляет исходный воксель и его зеркальную пару по оси X.
     * 
     * @return Количество удаленных вокселей (1 или 2)
     */
    public static int removeSymmetricalX(VoxelGrid grid, int x, int y, int z) {
        int removed = 0;
        if (grid.get(x, y, z) != VoxelConstants.EMPTY_MATERIAL_ID) {
            grid.remove(x, y, z);
            removed++;
        }

        int mx = mirrorX(x);
        if (mx != x && grid.get(mx, y, z) != VoxelConstants.EMPTY_MATERIAL_ID) {
            grid.remove(mx, y, z);
            removed++;
        }
        return removed;
    }

    /**
     * Зеркальная установка пластины по оси X.
     * 
     * @return Общее число установленных вокселей в обеих сторонах
     */
    public static int placePlateSymmetricalX(VoxelGrid grid,
                                            int originX, int originY, int originZ,
                                            int[] stepU, int[] stepV,
                                            int width, int height, byte materialId) {
        // Установка первой пластины
        int placed = grid.placePlate(originX, originY, originZ, stepU, stepV, width, height, materialId);

        // Расчет параметров для зеркальной пластины
        // Зеркалим начальную точку X и направление вектора U/V по X
        int mirrorOriginX = mirrorX(originX);
        int[] mirrorStepU = new int[] { -stepU[0], stepU[1], stepU[2] };
        int[] mirrorStepV = new int[] { -stepV[0], stepV[1], stepV[2] };

        placed += grid.placePlate(mirrorOriginX, originY, originZ, mirrorStepU, mirrorStepV, width, height, materialId);
        return placed;
    }
}
