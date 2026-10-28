package com.voxelstand.client.render;

import com.voxelstand.api.ItemPoseData;
import com.voxelstand.api.VoxelConstants;
import com.voxelstand.api.VoxelDataHelper;
import com.voxelstand.mesh.GreedyMesher;
import com.voxelstand.mesh.VoxelQuad;
import com.voxelstand.voxel.VoxelCompressor;
import com.voxelstand.voxel.VoxelGrid;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Рендерер кастомных 3D воксельных предметов в руках (1st и 3rd person), инвентаре (GUI) и на земле.
 */
public class CustomItemRenderer {
    private static final Map<Integer, List<VoxelQuad>> ITEM_MESH_CACHE = new HashMap<>();

    public static boolean renderCustomItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                                           MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (stack.isEmpty()) return false;

        CompoundTag customData = VoxelDataHelper.getCustomData(stack);
        byte[] data = null;

        if (customData.contains(VoxelConstants.NBT_VOXEL_DATA)) {
            data = customData.getByteArray(VoxelConstants.NBT_VOXEL_DATA);
        } else if (customData.contains(VoxelConstants.NBT_VOXEL_MODEL)) {
            CompoundTag modelTag = customData.getCompound(VoxelConstants.NBT_VOXEL_MODEL);
            for (String key : new String[]{"body", "head", "pelvis", "left_boot"}) {
                if (modelTag.contains(key)) {
                    data = modelTag.getByteArray(key);
                    break;
                }
            }
            if (data == null && !modelTag.isEmpty()) {
                String firstKey = modelTag.getAllKeys().iterator().next();
                data = modelTag.getByteArray(firstKey);
            }
        }

        if (data == null || data.length == 0) return false;

        int hash = java.util.Arrays.hashCode(data);
        List<VoxelQuad> quads = ITEM_MESH_CACHE.computeIfAbsent(hash, k -> {
            VoxelGrid grid = VoxelCompressor.decompress(k == hash ? customData.contains(VoxelConstants.NBT_VOXEL_DATA) ? customData.getByteArray(VoxelConstants.NBT_VOXEL_DATA) : customData.getCompound(VoxelConstants.NBT_VOXEL_MODEL).getByteArray(customData.getCompound(VoxelConstants.NBT_VOXEL_MODEL).getAllKeys().iterator().next()) : new byte[0]);
            return GreedyMesher.mesh(grid);
        });

        if (quads.isEmpty()) return false;

        poseStack.pushPose();

        boolean isArmor = customData.contains(VoxelConstants.NBT_VOXEL_MODEL);
        boolean isGui = (context == ItemDisplayContext.GUI);
        boolean isHand = (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ||
                          context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND);

        // Точка хвата: для оружия в руках — рукоять (5, 5, 16); в GUI и на земле — центр (16, 16, 16)
        float pivotX = (!isArmor && isHand) ? 5.0f : 16.0f;
        float pivotY = (!isArmor && isHand) ? 5.0f : 16.0f;
        float pivotZ = 16.0f;

        // 1. Позиционирование и поворот в зависимости от контекста отображения
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.5, 0.5, 0.5);
            if (isArmor) {
                // Броня в слоте инвентаря: развернута передом к игроку
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
                poseStack.scale(0.7f, 0.7f, 0.7f);
            } else {
                // Оружие и инструменты в слоте инвентаря: абсолютно аутентичное яркое 2D отображение
                poseStack.scale(0.95f, 0.95f, 0.95f);
            }
        } else if (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            // От первого лица: хват за рукоять, клинок направлен вперед
            poseStack.translate(0.55, 0.22, 0.55);
            if (!isArmor) {
                poseStack.mulPose(Axis.YP.rotationDegrees(-90.0f));
                poseStack.mulPose(Axis.ZP.rotationDegrees(25.0f));
            } else {
                poseStack.mulPose(Axis.XP.rotationDegrees(-15.0f));
            }
            poseStack.scale(0.85f, 0.85f, 0.85f);
        } else if (context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            // В руке от третьего лица: хват за рукоять, боевой угол 55 градусов
            poseStack.translate(0.50, 0.25, 0.50);
            if (!isArmor) {
                poseStack.mulPose(Axis.YP.rotationDegrees(-90.0f));
                poseStack.mulPose(Axis.ZP.rotationDegrees(55.0f));
            } else {
                poseStack.mulPose(Axis.XP.rotationDegrees(-15.0f));
            }
            poseStack.scale(0.85f, 0.85f, 0.85f);
        } else if (context == ItemDisplayContext.GROUND) {
            // На земле (выброшенный предмет лежит плоско)
            poseStack.translate(0.5, 0.05, 0.5);
            if (!isArmor) {
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
            }
            poseStack.scale(0.65f, 0.65f, 0.65f);
        } else {
            // Рамки и прочее
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.scale(0.8f, 0.8f, 0.8f);
        }

        // Применение позы удержания (хват, вращение, наклон)
        if (context != ItemDisplayContext.GUI && customData.contains(VoxelConstants.NBT_POSE_DATA)) {
            ItemPoseData pose = ItemPoseData.fromNbt(customData.getCompound(VoxelConstants.NBT_POSE_DATA));
            float voxScale = 1.0f / 32.0f;
            poseStack.translate(pose.gripOffsetX * voxScale, pose.gripOffsetY * voxScale, pose.gripOffsetZ * voxScale);
            if (pose.itemRotX != 0.0f) poseStack.mulPose(Axis.XP.rotationDegrees(pose.itemRotX));
            if (pose.itemRotY != 0.0f) poseStack.mulPose(Axis.YP.rotationDegrees(pose.itemRotY));
            if (pose.itemRotZ != 0.0f) poseStack.mulPose(Axis.ZP.rotationDegrees(pose.itemRotZ));
        }

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        Matrix4f mat = poseStack.last().pose();
        float scale = 1.0f / 32.0f;

        // Расчет света: в GUI максимальная яркость, в мире — с гарантированным фоновым освещением
        int effectiveLight = isGui ? 0xF000F0 : Math.max(packedLight, 0x00B000B0);

        for (VoxelQuad quad : quads) {
            int light = quad.emissive ? 0xF000F0 : effectiveLight;

            float x1 = (quad.x1 - pivotX) * scale;
            float y1 = (quad.y1 - pivotY) * scale;
            float z1 = (quad.z1 - pivotZ) * scale;

            float x2 = (quad.x2 - pivotX) * scale;
            float y2 = (quad.y2 - pivotY) * scale;
            float z2 = (quad.z2 - pivotZ) * scale;

            float r = quad.getRed(), g = quad.getGreen(), b = quad.getBlue();
            VoxelRenderHelper.renderFace(buffer, mat, x1, y1, z1, x2, y2, z2, quad.direction,
                    r, g, b, 1.0f, light, packedOverlay, isGui);
        }

        poseStack.popPose();
        return true;
    }
}
