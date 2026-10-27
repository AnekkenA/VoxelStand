package com.voxelstand.api;

public final class VoxelConstants {
    private VoxelConstants() {}

    /** Базовый размер сетки по каждой оси */
    public static final int GRID_SIZE = 32;

    /** Общее число ячеек в кубе 32x32x32 */
    public static final int TOTAL_VOXELS = GRID_SIZE * GRID_SIZE * GRID_SIZE; // 32768

    /** Максимальный лимит установленных вокселей на один предмет */
    public static final int DEFAULT_VOXEL_LIMIT = 4096;

    /** Количество бит на один воксель (2^5 = 32 состояния: 0..31) */
    public static final int BITS_PER_VOXEL = 5;

    /** Маска для 5 бит (0b11111 = 0x1F = 31) */
    public static final int MATERIAL_MASK = 0x1F;

    /** ID пустого вокселя (воздух) */
    public static final byte EMPTY_MATERIAL_ID = 0;

    /** Максимальный ID материала */
    public static final byte MAX_MATERIAL_ID = 31;

    /** NBT-ключ для хранения воксельной модели в ItemStack */
    public static final String NBT_VOXEL_MODEL = "VoxelStandModel";
    public static final String NBT_VOXEL_DATA = "VoxelData";
    public static final String NBT_SYMMETRY_ENABLED = "SymmetryMode";
    public static final String NBT_PART_NAME = "BodyPart";

    /** NBT-ключи для свободной позы и хвата */
    public static final String NBT_POSE_DATA = "CustomSmithPose";
    public static final String NBT_ARM_PITCH = "ArmPitch";
    public static final String NBT_ARM_YAW = "ArmYaw";
    public static final String NBT_ARM_ROLL = "ArmRoll";
    public static final String NBT_ITEM_ROT_X = "ItemRotX";
    public static final String NBT_ITEM_ROT_Y = "ItemRotY";
    public static final String NBT_ITEM_ROT_Z = "ItemRotZ";
    public static final String NBT_GRIP_OFFSET_X = "GripOffsetX";
    public static final String NBT_GRIP_OFFSET_Y = "GripOffsetY";
    public static final String NBT_GRIP_OFFSET_Z = "GripOffsetZ";
    public static final String NBT_TWO_HANDED = "TwoHanded";
}
