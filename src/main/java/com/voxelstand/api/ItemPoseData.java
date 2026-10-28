package com.voxelstand.api;

import net.minecraft.nbt.CompoundTag;

/**
 * Данные свободной позы удержания предмета в руках персонажа.
 * Позволяет свободно настраивать:
 * - 3 угла вращения плеча (ArmPitch, ArmYaw, ArmRoll).
 * - 3 угла вращения самого предмета в ладони (ItemRotX, ItemRotY, ItemRotZ).
 * - 3 координаты сдвига хвата сквозь ладонь (GripOffsetX, GripOffsetY, GripOffsetZ).
 * - Режим двуручного удержания (TwoHanded).
 */
public class ItemPoseData {
    // Углы вращения плеча игрока (в градусах)
    public float armPitch = 0.0f;  // Наклон руки вверх/вниз (-90..90)
    public float armYaw = 0.0f;    // Отведение руки вбок / сведение к груди (-90..90)
    public float armRoll = 0.0f;   // Вращение руки вокруг оси

    // Вращение предмета внутри ладони (в градусах)
    public float itemRotX = 0.0f;
    public float itemRotY = 0.0f;
    public float itemRotZ = 0.0f;

    // Смещение точки хвата (в вокселях, относительно центра)
    public float gripOffsetX = 0.0f;
    public float gripOffsetY = 0.0f;
    public float gripOffsetZ = 0.0f;

    // Двуручный хват
    public boolean twoHanded = false;

    public ItemPoseData() {}

    public static ItemPoseData createDefault(float totalWeight, boolean isShield) {
        ItemPoseData pose = new ItemPoseData();
        if (isShield) {
            pose.armPitch = -15.0f;
            pose.itemRotY = 90.0f;
            pose.twoHanded = false;
        } else if (totalWeight > 350.0f) {
            // Тяжелый предмет (молот, алебарда): рекомендуется двуручный хват
            pose.armPitch = -25.0f;
            pose.armYaw = -10.0f;
            pose.twoHanded = true;
            pose.gripOffsetY = -4.0f;
        } else {
            // Обычный одноручный хват
            pose.armPitch = 0.0f;
            pose.armYaw = 0.0f;
            pose.twoHanded = false;
        }
        return pose;
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat(VoxelConstants.NBT_ARM_PITCH, armPitch);
        tag.putFloat(VoxelConstants.NBT_ARM_YAW, armYaw);
        tag.putFloat(VoxelConstants.NBT_ARM_ROLL, armRoll);
        tag.putFloat(VoxelConstants.NBT_ITEM_ROT_X, itemRotX);
        tag.putFloat(VoxelConstants.NBT_ITEM_ROT_Y, itemRotY);
        tag.putFloat(VoxelConstants.NBT_ITEM_ROT_Z, itemRotZ);
        tag.putFloat(VoxelConstants.NBT_GRIP_OFFSET_X, gripOffsetX);
        tag.putFloat(VoxelConstants.NBT_GRIP_OFFSET_Y, gripOffsetY);
        tag.putFloat(VoxelConstants.NBT_GRIP_OFFSET_Z, gripOffsetZ);
        tag.putBoolean(VoxelConstants.NBT_TWO_HANDED, twoHanded);
        return tag;
    }

    public void copyFrom(ItemPoseData other) {
        if (other == null) return;
        this.armPitch = other.armPitch;
        this.armYaw = other.armYaw;
        this.armRoll = other.armRoll;
        this.itemRotX = other.itemRotX;
        this.itemRotY = other.itemRotY;
        this.itemRotZ = other.itemRotZ;
        this.gripOffsetX = other.gripOffsetX;
        this.gripOffsetY = other.gripOffsetY;
        this.gripOffsetZ = other.gripOffsetZ;
        this.twoHanded = other.twoHanded;
    }

    public static ItemPoseData fromNbt(CompoundTag tag) {
        ItemPoseData pose = new ItemPoseData();
        if (tag == null) return pose;

        pose.armPitch = tag.getFloat(VoxelConstants.NBT_ARM_PITCH);
        pose.armYaw = tag.getFloat(VoxelConstants.NBT_ARM_YAW);
        pose.armRoll = tag.getFloat(VoxelConstants.NBT_ARM_ROLL);
        pose.itemRotX = tag.getFloat(VoxelConstants.NBT_ITEM_ROT_X);
        pose.itemRotY = tag.getFloat(VoxelConstants.NBT_ITEM_ROT_Y);
        pose.itemRotZ = tag.getFloat(VoxelConstants.NBT_ITEM_ROT_Z);
        pose.gripOffsetX = tag.getFloat(VoxelConstants.NBT_GRIP_OFFSET_X);
        pose.gripOffsetY = tag.getFloat(VoxelConstants.NBT_GRIP_OFFSET_Y);
        pose.gripOffsetZ = tag.getFloat(VoxelConstants.NBT_GRIP_OFFSET_Z);
        pose.twoHanded = tag.getBoolean(VoxelConstants.NBT_TWO_HANDED);
        return pose;
    }
}
