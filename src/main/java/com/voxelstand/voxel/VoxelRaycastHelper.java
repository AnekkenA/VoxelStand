package com.voxelstand.voxel;

import com.voxelstand.block.SculptStandBlock;
import com.voxelstand.block.entity.SculptStandBlockEntity;
import com.voxelstand.client.render.SculptStandRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * 3D воксельный рейкаст для стапеля кузницы.
 * Преобразует вектор взгляда игрока из мировых координат в воксельную сетку 32x32x32
 * и находит точный воксел и смежную грань для скалывания долотом, смены тона или наложения пластин.
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

        String defaultPart = isWeapon ? "weapon" :
                (mode == SculptStandBlockEntity.StandMode.HELMET ? "head" :
                (mode == SculptStandBlockEntity.StandMode.CHESTPLATE ? "body" :
                (mode == SculptStandBlockEntity.StandMode.LEGGINGS ? "pelvis" : "left_boot")));

        Map<String, VoxelGrid> allGrids = stand.getAllGrids();
        if (allGrids.isEmpty()) return null;

        double maxDist = 5.0;
        double step = 0.015; // Высокая точность выборки

        int lastVx = -1, lastVy = -1, lastVz = -1;
        String lastPart = defaultPart;

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

            if (isWeapon) {
                // Обратное преобразование оружия:
                // forward: translate(0, 0.1125, 0) -> rotZ(-90) -> translate(0, -0.5, 0)
                double noTy = ty - 0.1125;
                double rotX = -noTy;
                double rotY = tx + 0.5;
                double rotZ = tz;

                int vx = (int) Math.floor(rotX * 32.0 + 16.0);
                int vy = (int) Math.floor(rotY * 32.0);
                int vz = (int) Math.floor(rotZ * 32.0 + 16.0);

                VoxelGrid g = allGrids.get("weapon");
                if (g != null && VoxelGrid.isInBounds(vx, vy, vz)) {
                    if (g.get(vx, vy, vz) != 0) {
                        int px = lastVx >= 0 ? lastVx : vx;
                        int py = lastVy >= 0 ? lastVy : vy;
                        int pz = lastVz >= 0 ? lastVz : vz;
                        return new RaycastHit(vx, vy, vz, px, py, pz, Direction.UP, "weapon");
                    }
                    lastVx = vx;
                    lastVy = vy;
                    lastVz = vz;
                    lastPart = "weapon";
                }
            } else {
                // Проверяем каждую деталь брони с её собственным сдвигом
                for (Map.Entry<String, VoxelGrid> entry : allGrids.entrySet()) {
                    String part = entry.getKey();
                    VoxelGrid g = entry.getValue();
                    if (g == null || g.getNonEmptyCount() == 0) continue;

                    double partShift = 0.0;
                    if (part.contains("arm")) {
                        partShift = 0.375;
                    } else if (part.contains("leg") || part.contains("boot")) {
                        partShift = 0.15625;
                    }

                    double ptx = tx;
                    if (part.contains("left")) {
                        ptx = tx + partShift;
                    } else if (part.contains("right")) {
                        ptx = tx - partShift;
                    }

                    int vx = (int) Math.floor(ptx * 32.0 + 16.0);
                    int vy = (int) Math.floor(ty * 32.0);
                    int vz = (int) Math.floor(tz * 32.0 + 16.0);

                    if (VoxelGrid.isInBounds(vx, vy, vz)) {
                        if (g.get(vx, vy, vz) != 0) {
                            int px = lastVx >= 0 ? lastVx : vx;
                            int py = lastVy >= 0 ? lastVy : vy;
                            int pz = lastVz >= 0 ? lastVz : vz;
                            return new RaycastHit(vx, vy, vz, px, py, pz, Direction.UP, part);
                        }
                        if (part.equals(defaultPart)) {
                            lastVx = vx;
                            lastVy = vy;
                            lastVz = vz;
                            lastPart = part;
                        }
                    }
                }
            }
        }

        // Если прямого попадания в воксел не было, возвращаем координату рабочей области стола
        if (lastVx >= 0 && lastVz >= 0) {
            int py = Math.max(0, Math.min(31, lastVy));
            return new RaycastHit(lastVx, py, lastVz, lastVx, py, lastVz, Direction.UP, lastPart);
        }

        return null;
    }
}
