package com.voxelstand.voxel;

import com.voxelstand.block.SculptStandBlock;
import com.voxelstand.block.entity.SculptStandBlockEntity;
import com.voxelstand.client.render.SculptStandRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 3D воксельный рейкаст для стапеля кузницы.
 * Преобразует вектор взгляда игрока из мировых координат в воксельную сетку 32x32x32
 * и находит точный воксел и смежную грань для скалывания долотом или наложения пластин.
 */
public class VoxelRaycastHelper {

    public static class RaycastHit {
        public final int vx, vy, vz;          // Координаты пораженного вокселя
        public final int placeX, placeY, placeZ; // Координаты для установки смежной пластины
        public final Direction face;          // Направление грани удара
        public final String partName;         // Имя детали ("weapon", "body", "head" и т.д.)

        public RaycastHit(int vx, int vy, int vz, int px, int py, int pz, Direction face, String partName) {
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.placeX = px;
            this.placeY = py;
            this.placeZ = pz;
            this.face = face;
            this.partName = partName;
        }
    }

    public static RaycastHit raycast(Player player, SculptStandBlockEntity stand, BlockPos pos) {
        Direction facing = stand.getBlockState().hasProperty(SculptStandBlock.FACING) ?
                stand.getBlockState().getValue(SculptStandBlock.FACING) : Direction.NORTH;
        float yRot = SculptStandRenderer.getBlockstateYRot(facing);
        double rad = Math.toRadians(yRot);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);

        SculptStandBlockEntity.StandMode mode = stand.getMode();
        boolean isWeapon = mode == SculptStandBlockEntity.StandMode.WEAPON;

        // Поиск основной редактируемой детали
        String mainPart = isWeapon ? "weapon" :
                (mode == SculptStandBlockEntity.StandMode.HELMET ? "head" :
                (mode == SculptStandBlockEntity.StandMode.CHESTPLATE ? "body" :
                (mode == SculptStandBlockEntity.StandMode.LEGGINGS ? "pelvis" : "left_boot")));

        VoxelGrid grid = stand.getGrid(mainPart);
        if (grid == null) return null;

        int prevVx = -1, prevVy = -1, prevVz = -1;

        // Трассировка луча с шагом 0.02 блока (~0.6 вокселя) на дистанцию до 4.5 блоков
        double maxDist = 4.5;
        double step = 0.02;

        for (double d = 0.5; d <= maxDist; d += step) {
            double wx = eye.x + look.x * d;
            double wy = eye.y + look.y * d;
            double wz = eye.z + look.z * d;

            // 1. Сдвиг относительно базового блока
            double relX = wx - pos.getX();
            double relY = wy - pos.getY();
            double relZ = wz - pos.getZ();

            // 2. Обратный поворот вокруг центра блока (0.5, 0.5)
            double dx = relX - 0.5;
            double dz = relZ - 0.5;
            double rx = dx * cos - dz * sin + 0.5;
            double rz = dx * sin + dz * cos + 0.5;
            double ry = relY;

            // 3. Сдвиг центра столешницы (1.0, 0.95, 0.5)
            double tx = rx - 1.0;
            double ty = ry - 0.95;
            double tz = rz - 0.5;

            double localX, localY, localZ;

            if (isWeapon) {
                // Обратное преобразование оружия:
                // В рендерере: translate(0, 0.1125, 0), rotZ(-90), translate(0, -0.5, 0)
                double noTy = ty - 0.1125;
                // Обратный поворот Z: rotZ(+90)
                // x' = -y, y' = x
                double rotX = -noTy;
                double rotY = tx;
                double rotZ = tz;

                // Обратный translate(0, -0.5, 0)
                rotY = rotY + 0.5;

                localX = rotX;
                localY = rotY;
                localZ = rotZ;
            } else {
                localX = tx;
                localY = ty;
                localZ = tz;
            }

            int vx = (int) Math.floor(localX * 32.0 + 16.0);
            int vy = (int) Math.floor(localY * 32.0);
            int vz = (int) Math.floor(localZ * 32.0 + 16.0);

            if (VoxelGrid.isInBounds(vx, vy, vz)) {
                if (grid.get(vx, vy, vz) != 0) {
                    // Попадание в существующий воксел!
                    int px = prevVx >= 0 ? prevVx : vx;
                    int py = prevVy >= 0 ? prevVy : vy;
                    int pz = prevVz >= 0 ? prevVz : vz;

                    Direction hitFace = Direction.UP;
                    if (prevVx > vx) hitFace = Direction.EAST;
                    else if (prevVx < vx && prevVx >= 0) hitFace = Direction.WEST;
                    else if (prevVy > vy) hitFace = Direction.UP;
                    else if (prevVy < vy && prevVy >= 0) hitFace = Direction.DOWN;
                    else if (prevVz > vz) hitFace = Direction.SOUTH;
                    else if (prevVz < vz && prevVz >= 0) hitFace = Direction.NORTH;

                    return new RaycastHit(vx, vy, vz, px, py, pz, hitFace, mainPart);
                }
                prevVx = vx;
                prevVy = vy;
                prevVz = vz;
            }
        }

        // Если не попали в воксел, но игрок смотрит на рабочую зону стола:
        if (prevVx >= 0 && prevVz >= 0) {
            int py = Math.max(0, Math.min(31, prevVy));
            return new RaycastHit(prevVx, py, prevVz, prevVx, py, prevVz, Direction.UP, mainPart);
        }

        return null;
    }
}
