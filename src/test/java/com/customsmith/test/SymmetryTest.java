package com.voxelstand.test;

import com.voxelstand.voxel.SymmetryHelper;
import com.voxelstand.voxel.VoxelGrid;
import com.voxelstand.voxel.VoxelMaterial;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SymmetryTest {

    @Test
    public void testMirrorCoordinates() {
        Assertions.assertEquals(31, SymmetryHelper.mirrorX(0));
        Assertions.assertEquals(0, SymmetryHelper.mirrorX(31));
        Assertions.assertEquals(16, SymmetryHelper.mirrorX(15));
        Assertions.assertEquals(15, SymmetryHelper.mirrorX(16));
        Assertions.assertEquals(21, SymmetryHelper.mirrorX(10));
    }

    @Test
    public void testSymmetricalChiselRemoval() {
        VoxelGrid grid = new VoxelGrid();

        // Ставим воксель слева и справа
        grid.set(5, 10, 10, VoxelMaterial.IRON.getId());
        grid.set(26, 10, 10, VoxelMaterial.IRON.getId());
        Assertions.assertEquals(2, grid.getNonEmptyCount());

        // Скалываем левый с включенной симметрией
        int removed = SymmetryHelper.removeSymmetricalX(grid, 5, 10, 10);
        Assertions.assertEquals(2, removed);
        Assertions.assertEquals(0, grid.getNonEmptyCount(), "Оба симметричных вокселя должны быть удалены!");
    }

    @Test
    public void testSymmetricalPlatePlacement() {
        VoxelGrid grid = new VoxelGrid();

        int[] stepU = {1, 0, 0};
        int[] stepV = {0, 1, 0};

        // Ставим пластину 4x4 от X=0
        int placed = SymmetryHelper.placePlateSymmetricalX(grid, 0, 0, 0, stepU, stepV, 4, 4, VoxelMaterial.COPPER.getId());

        // Должно быть установлено 16 (слева) + 16 (справа) = 32 вокселя
        Assertions.assertEquals(32, placed);
        Assertions.assertEquals(32, grid.getNonEmptyCount());

        // Проверяем, что слева заняты X от 0 до 3
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                Assertions.assertEquals(VoxelMaterial.COPPER.getId(), grid.get(x, y, 0));
            }
        }

        // Проверяем, что зеркально справа заняты X от 28 до 31
        for (int x = 28; x < 32; x++) {
            for (int y = 0; y < 4; y++) {
                Assertions.assertEquals(VoxelMaterial.COPPER.getId(), grid.get(x, y, 0));
            }
        }
    }
}
