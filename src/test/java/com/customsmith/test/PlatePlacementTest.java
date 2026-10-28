package com.voxelstand.test;

import com.voxelstand.voxel.VoxelGrid;
import com.voxelstand.voxel.VoxelMaterial;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PlatePlacementTest {

    @Test
    public void testPlatePlacementDoesNotOverwriteExistingVoxels() {
        VoxelGrid grid = new VoxelGrid();

        // 1. Предварительно ставим алмазную точку в [5, 5, 0]
        grid.set(5, 5, 0, VoxelMaterial.DIAMOND.getId());
        Assertions.assertEquals(1, grid.getNonEmptyCount());

        // 2. Накладываем поверх железную пластину 8x8 в область [0..7, 0..7, 0]
        int[] stepU = {1, 0, 0};
        int[] stepV = {0, 1, 0};
        int placed = grid.placePlate(0, 0, 0, stepU, stepV, 8, 8, VoxelMaterial.IRON.getId());

        // Из 64 ячеек 1 уже была занята, поэтому должно установиться ровно 63 вокселя
        Assertions.assertEquals(63, placed);
        Assertions.assertEquals(64, grid.getNonEmptyCount());

        // Алмазный воксель НЕ должен был перезаписаться железом!
        Assertions.assertEquals(VoxelMaterial.DIAMOND.getId(), grid.get(5, 5, 0),
                "Существующий воксель не должен заменяться новой пластиной!");

        // Остальные воксели пластины должны быть железными
        Assertions.assertEquals(VoxelMaterial.IRON.getId(), grid.get(0, 0, 0));
        Assertions.assertEquals(VoxelMaterial.IRON.getId(), grid.get(7, 7, 0));
    }

    @Test
    public void testPlatePlacementOutOfBoundsClipping() {
        VoxelGrid grid = new VoxelGrid();

        int[] stepU = {1, 0, 0};
        int[] stepV = {0, 1, 0};

        // Ставим пластину 8x8 в [28, 28, 0], часть выходит за границы сетки 32x32
        // Внутрь сетки попадают только X=[28..31] (4) и Y=[28..31] (4) => 4 * 4 = 16 вокселей
        int placed = grid.placePlate(28, 28, 0, stepU, stepV, 8, 8, VoxelMaterial.NETHERITE.getId());

        Assertions.assertEquals(16, placed, "Должны установиться только воксели внутри диапазона 0..31");
        Assertions.assertEquals(16, grid.getNonEmptyCount());
    }
}
