package com.voxelstand.test;

import com.voxelstand.api.VoxelConstants;
import com.voxelstand.voxel.VoxelCompressor;
import com.voxelstand.voxel.VoxelGrid;
import com.voxelstand.voxel.VoxelMaterial;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;

public class VoxelCompressionTest {

    @Test
    public void testEmptyGridCompression() {
        VoxelGrid grid = new VoxelGrid();
        byte[] compressed = VoxelCompressor.compress(grid);

        Assertions.assertNotNull(compressed);
        Assertions.assertTrue(compressed.length < 50, "Пустая сетка должна сжиматься менее чем в 50 байт!");

        VoxelGrid restored = VoxelCompressor.decompress(compressed);
        Assertions.assertEquals(0, restored.getNonEmptyCount());
    }

    @Test
    public void testRandomModelIntegrityAndSize() {
        VoxelGrid grid = new VoxelGrid();
        Random random = new Random(42);

        // Размещаем 3000 случайных вокселей разных материалов
        int placedCount = 0;
        for (int i = 0; i < 3000; i++) {
            int x = random.nextInt(VoxelConstants.GRID_SIZE);
            int y = random.nextInt(VoxelConstants.GRID_SIZE);
            int z = random.nextInt(VoxelConstants.GRID_SIZE);
            byte mat = (byte) (1 + random.nextInt(VoxelConstants.MAX_MATERIAL_ID));

            if (grid.set(x, y, z, mat)) {
                placedCount++;
            }
        }

        byte[] compressed = VoxelCompressor.compress(grid);
        Assertions.assertNotNull(compressed);
        // Проверяем, что размер сжатия безопасен для мультиплеерных пакетов Minecraft (< 5 КБ)
        Assertions.assertTrue(compressed.length < 5000, "Сжатая модель должна быть компактной (< 5 КБ), факт: " + compressed.length);

        // Распаковка и повоксельная сверка 32768 ячеек
        VoxelGrid restored = VoxelCompressor.decompress(compressed);
        Assertions.assertEquals(placedCount, restored.getNonEmptyCount());

        for (int x = 0; x < VoxelConstants.GRID_SIZE; x++) {
            for (int y = 0; y < VoxelConstants.GRID_SIZE; y++) {
                for (int z = 0; z < VoxelConstants.GRID_SIZE; z++) {
                    Assertions.assertEquals(grid.get(x, y, z), restored.get(x, y, z),
                            String.format("Несовпадение вокселя в [%d, %d, %d]", x, y, z));
                }
            }
        }
    }
}
