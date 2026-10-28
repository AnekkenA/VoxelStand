package com.voxelstand.test;

import com.voxelstand.mesh.GreedyMesher;
import com.voxelstand.mesh.VoxelQuad;
import com.voxelstand.voxel.VoxelGrid;
import com.voxelstand.voxel.VoxelMaterial;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class GreedyMesherTest {

    @Test
    public void testSingleVoxelMeshing() {
        VoxelGrid grid = new VoxelGrid();
        grid.set(5, 5, 5, VoxelMaterial.IRON.getId());

        List<VoxelQuad> quads = GreedyMesher.mesh(grid);
        // Одиночный кубик должен иметь ровно 6 граней (по одной на каждое направление)
        Assertions.assertEquals(6, quads.size(), "Одиночный воксель должен давать ровно 6 квадов!");
    }

    @Test
    public void testSolidCubeFaceCullingAndMerging() {
        VoxelGrid grid = new VoxelGrid();

        // Строим сплошной куб 2x2x2 (8 вокселей = 48 потенциальных граней)
        for (int x = 10; x < 12; x++) {
            for (int y = 10; y < 12; y++) {
                for (int z = 10; z < 12; z++) {
                    grid.set(x, y, z, VoxelMaterial.GOLD.getId());
                }
            }
        }

        List<VoxelQuad> quads = GreedyMesher.mesh(grid);
        // С жадным мешингом и отсечением внутренних граней должно получиться ровно 6 квадов!
        Assertions.assertEquals(6, quads.size(),
                "Куб 2x2x2 должен объединяться ровно в 6 квадов вместо 48 граней!");
    }

    @Test
    public void testFlatPlateMerging() {
        VoxelGrid grid = new VoxelGrid();

        // Пластина 16x16x1 (256 вокселей)
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                grid.set(x, 0, z, VoxelMaterial.OBSIDIAN.getId());
            }
        }

        List<VoxelQuad> quads = GreedyMesher.mesh(grid);
        // Верх: 1 квад, Низ: 1 квад, 4 боковых ребра: по 1 кваду на сторону = всего 6 квадов!
        Assertions.assertEquals(6, quads.size(),
                "Пластина 16x16 должна объединяться ровно в 6 квадов вместо 1536 граней!");
    }

    @Test
    public void testDifferentMaterialsDoNotMerge() {
        VoxelGrid grid = new VoxelGrid();

        // Два смежных вокселя РАЗНЫХ материалов
        grid.set(5, 5, 5, VoxelMaterial.DIAMOND.getId());
        grid.set(6, 5, 5, VoxelMaterial.REDSTONE.getId());

        List<VoxelQuad> quads = GreedyMesher.mesh(grid);
        // Внутренняя грань между разными материалами должна отсекаться или разделяться,
        // но наружные грани не должны объединяться в один цвет.
        long diamondQuads = quads.stream().filter(q -> q.materialId == VoxelMaterial.DIAMOND.getId()).count();
        long redstoneQuads = quads.stream().filter(q -> q.materialId == VoxelMaterial.REDSTONE.getId()).count();

        Assertions.assertTrue(diamondQuads >= 5, "У алмазного вокселя должно быть минимум 5 внешних граней");
        Assertions.assertTrue(redstoneQuads >= 5, "У редстоунового вокселя должно быть минимум 5 внешних граней");
    }
}
