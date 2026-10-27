package com.voxelstand.client.raycast;

import com.voxelstand.api.VoxelConstants;
import com.voxelstand.mesh.VoxelFaceDirection;
import com.voxelstand.voxel.VoxelGrid;
import net.minecraft.world.phys.Vec3;

/**
 * 3D DDA (Digital Differential Analyzer) алгоритм трассировки луча взгляда игрока
 * сквозь воксельную микросетку 32x32x32.
 */
public final class VoxelRaycastHelper {
    private VoxelRaycastHelper() {}

    public static class VoxelHitResult {
        public final int x, y, z;
        public final VoxelFaceDirection face;
        public final Vec3 hitPoint;

        public VoxelHitResult(int x, int y, int z, VoxelFaceDirection face, Vec3 hitPoint) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.face = face;
            this.hitPoint = hitPoint;
        }
    }

    /**
     * Поиск первого твердого вокселя на пути луча взгляда.
     * 
     * @param origin Начало луча (глаза игрока в координатах сетки от 0 до 32)
     * @param direction Направление взгляда (единичный вектор)
     * @param maxDistance Максимальная дистанция трассировки
     * @param grid Воксельная сетка
     * @return Результат попадания или null
     */
    public static VoxelHitResult raycast(Vec3 origin, Vec3 direction, double maxDistance, VoxelGrid grid) {
        double px = origin.x;
        double py = origin.y;
        double pz = origin.z;

        double dx = direction.x;
        double dy = direction.y;
        double dz = direction.z;

        int vx = (int) Math.floor(px);
        int vy = (int) Math.floor(py);
        int vz = (int) Math.floor(pz);

        int stepX = dx > 0 ? 1 : (dx < 0 ? -1 : 0);
        int stepY = dy > 0 ? 1 : (dy < 0 ? -1 : 0);
        int stepZ = dz > 0 ? 1 : (dz < 0 ? -1 : 0);

        double tDeltaX = (stepX != 0) ? Math.abs(1.0 / dx) : Double.MAX_VALUE;
        double tDeltaY = (stepY != 0) ? Math.abs(1.0 / dy) : Double.MAX_VALUE;
        double tDeltaZ = (stepZ != 0) ? Math.abs(1.0 / dz) : Double.MAX_VALUE;

        double tMaxX = (stepX > 0) ? ((vx + 1.0) - px) * tDeltaX : (px - vx) * tDeltaX;
        double tMaxY = (stepY > 0) ? ((vy + 1.0) - py) * tDeltaY : (py - vy) * tDeltaY;
        double tMaxZ = (stepZ > 0) ? ((vz + 1.0) - pz) * tDeltaZ : (pz - vz) * tDeltaZ;

        double distance = 0.0;
        VoxelFaceDirection lastFace = null;

        while (distance <= maxDistance) {
            if (VoxelGrid.isInBounds(vx, vy, vz)) {
                if (grid.get(vx, vy, vz) != VoxelConstants.EMPTY_MATERIAL_ID) {
                    Vec3 hit = origin.add(direction.scale(distance));
                    return new VoxelHitResult(vx, vy, vz, lastFace != null ? lastFace : VoxelFaceDirection.UP, hit);
                }
            }

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    vx += stepX;
                    distance = tMaxX;
                    tMaxX += tDeltaX;
                    lastFace = (stepX > 0) ? VoxelFaceDirection.WEST : VoxelFaceDirection.EAST;
                } else {
                    vz += stepZ;
                    distance = tMaxZ;
                    tMaxZ += tDeltaZ;
                    lastFace = (stepZ > 0) ? VoxelFaceDirection.NORTH : VoxelFaceDirection.SOUTH;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    vy += stepY;
                    distance = tMaxY;
                    tMaxY += tDeltaY;
                    lastFace = (stepY > 0) ? VoxelFaceDirection.DOWN : VoxelFaceDirection.UP;
                } else {
                    vz += stepZ;
                    distance = tMaxZ;
                    tMaxZ += tDeltaZ;
                    lastFace = (stepZ > 0) ? VoxelFaceDirection.NORTH : VoxelFaceDirection.SOUTH;
                }
            }
        }

        return null;
    }
}
