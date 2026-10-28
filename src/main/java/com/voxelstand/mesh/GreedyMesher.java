package com.voxelstand.mesh;

import com.voxelstand.api.VoxelConstants;
import com.voxelstand.voxel.VoxelGrid;
import java.util.ArrayList;
import java.util.List;

/**
 * Реализация алгоритма жадного мешинга (Greedy Meshing) для воксельной сетки 32x32x32.
 * Отсекает внутренние скрытые грани (culling) и объединяет смежные грани одного цвета
 * в большие полигоны (quads), снижая число полигонов в 10-50 раз.
 */
public final class GreedyMesher {
    private GreedyMesher() {}

    private static class FaceInfo {
        byte materialId;
        boolean positiveDir;

        FaceInfo(byte materialId, boolean positiveDir) {
            this.materialId = materialId;
            this.positiveDir = positiveDir;
        }

        boolean equals(FaceInfo other) {
            if (other == null) return false;
            return this.materialId == other.materialId && this.positiveDir == other.positiveDir;
        }
    }

    public static List<VoxelQuad> mesh(VoxelGrid grid) {
        List<VoxelQuad> quads = new ArrayList<>();
        int s = VoxelConstants.GRID_SIZE;

        // Обход по трем главным осям: 0 = X, 1 = Y, 2 = Z
        for (int axis = 0; axis < 3; axis++) {
            int uAxis = (axis + 1) % 3;
            int vAxis = (axis + 2) % 3;

            int[] pos = new int[3];
            int[] dirStep = new int[3];
            dirStep[axis] = 1;

            // Срез вдоль выбранной оси (от -1 до s-1)
            for (pos[axis] = -1; pos[axis] < s; pos[axis]++) {
                // Создаем 2D маску среза
                FaceInfo[][] mask = new FaceInfo[s][s];

                for (pos[vAxis] = 0; pos[vAxis] < s; pos[vAxis]++) {
                    for (pos[uAxis] = 0; pos[uAxis] < s; pos[uAxis]++) {
                        byte a = (pos[axis] >= 0) ? grid.get(pos[0], pos[1], pos[2]) : VoxelConstants.EMPTY_MATERIAL_ID;
                        byte b = (pos[axis] + 1 < s) ? grid.get(pos[0] + dirStep[0], pos[1] + dirStep[1], pos[2] + dirStep[2]) : VoxelConstants.EMPTY_MATERIAL_ID;

                        boolean aSolid = (a != VoxelConstants.EMPTY_MATERIAL_ID);
                        boolean bSolid = (b != VoxelConstants.EMPTY_MATERIAL_ID);

                        if (aSolid != bSolid) {
                            mask[pos[uAxis]][pos[vAxis]] = new FaceInfo(
                                    aSolid ? a : b,
                                    aSolid // Если aSolid, то грань направлена в сторону +Axis, иначе -Axis
                            );
                        }
                    }
                }

                // Жадное объединение в прямоугольники на 2D маске
                for (int v = 0; v < s; v++) {
                    for (int u = 0; u < s; u++) {
                        FaceInfo current = mask[u][v];
                        if (current == null) continue;

                        // 1. Вычисляем ширину вдоль U
                        int width = 1;
                        while (u + width < s && current.equals(mask[u + width][v])) {
                            width++;
                        }

                        // 2. Вычисляем высоту вдоль V
                        int height = 1;
                        boolean canExpand = true;
                        while (v + height < s && canExpand) {
                            for (int k = 0; k < width; k++) {
                                if (!current.equals(mask[u + k][v + height])) {
                                    canExpand = false;
                                    break;
                                }
                            }
                            if (canExpand) {
                                height++;
                            }
                        }

                        // 3. Создаем квад
                        int[] p1 = new int[3];
                        int[] p2 = new int[3];

                        p1[axis] = pos[axis] + 1;
                        p1[uAxis] = u;
                        p1[vAxis] = v;

                        p2[axis] = pos[axis] + 1;
                        p2[uAxis] = u + width;
                        p2[vAxis] = v + height;

                        VoxelFaceDirection direction = getDirection(axis, current.positiveDir);

                        quads.add(new VoxelQuad(
                                p1[0], p1[1], p1[2],
                                p2[0], p2[1], p2[2],
                                direction,
                                current.materialId
                        ));

                        // 4. Очищаем обработанный прямоугольник в маске
                        for (int dv = 0; dv < height; dv++) {
                            for (int du = 0; du < width; du++) {
                                mask[u + du][v + dv] = null;
                            }
                        }

                        u += width - 1;
                    }
                }
            }
        }

        return quads;
    }

    private static VoxelFaceDirection getDirection(int axis, boolean positive) {
        if (axis == 0) return positive ? VoxelFaceDirection.EAST : VoxelFaceDirection.WEST;
        if (axis == 1) return positive ? VoxelFaceDirection.UP : VoxelFaceDirection.DOWN;
        return positive ? VoxelFaceDirection.SOUTH : VoxelFaceDirection.NORTH;
    }
}
