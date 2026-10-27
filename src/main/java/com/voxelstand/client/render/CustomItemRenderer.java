package com.voxelstand.client.render;

import com.voxelstand.api.ItemPoseData;
import com.voxelstand.api.VoxelConstants;
import com.voxelstand.mesh.GreedyMesher;
import com.voxelstand.mesh.VoxelQuad;
import com.voxelstand.voxel.VoxelCompressor;
import com.voxelstand.voxel.VoxelGrid;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Рендерер оружия с кастомной воксельной моделью в руке игрока и в виде изометрической 3D-модели в инвентаре.
 */
public class CustomItemRenderer {
    private static final Map<Integer, List<VoxelQuad>> ITEM_MESH_CACHE = new HashMap<>();

    public static boolean renderCustomItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                                           MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (stack.isEmpty() || !com.voxelstand.api.VoxelDataHelper.hasCustomKey(stack, VoxelConstants.NBT_VOXEL_DATA)) {
            return false;
        }

        CompoundTag customData = com.voxelstand.api.VoxelDataHelper.getCustomData(stack);
        byte[] data = customData.getByteArray(VoxelConstants.NBT_VOXEL_DATA);
        int hash = java.util.Arrays.hashCode(data);

        List<VoxelQuad> quads = ITEM_MESH_CACHE.computeIfAbsent(hash, k -> {
            VoxelGrid grid = VoxelCompressor.decompress(data);
            return GreedyMesher.mesh(grid);
        });

        if (quads.isEmpty()) return false;

        poseStack.pushPose();

        // 1. Позиционирование и поворот в зависимости от контекста
        if (context == ItemDisplayContext.GUI) {
            // Изометрический 3D-рендер в слоте инвентаря
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0f));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-25.0f));
            poseStack.scale(0.65f, 0.65f, 0.65f);
        } else if (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            // От первого лица
            poseStack.translate(0.5, 0.1, 0.5);
            poseStack.mulPose(Axis.XP.rotationDegrees(-15.0f));
            poseStack.scale(0.85f, 0.85f, 0.85f);
        } else {
            // От третьего лица и на земле
            poseStack.translate(0.5, 0.1, 0.5);
            poseStack.scale(0.75f, 0.75f, 0.75f);
        }

        // Применение свободной позы удержания и вращения предмета в руке
        if (context != ItemDisplayContext.GUI && customData.contains(VoxelConstants.NBT_POSE_DATA)) {
            ItemPoseData pose = ItemPoseData.fromNbt(customData.getCompound(VoxelConstants.NBT_POSE_DATA));
            float voxScale = 1.0f / 32.0f;
            poseStack.translate(pose.gripOffsetX * voxScale, pose.gripOffsetY * voxScale, pose.gripOffsetZ * voxScale);
            if (pose.itemRotX != 0.0f) poseStack.mulPose(Axis.XP.rotationDegrees(pose.itemRotX));
            if (pose.itemRotY != 0.0f) poseStack.mulPose(Axis.YP.rotationDegrees(pose.itemRotY));
            if (pose.itemRotZ != 0.0f) poseStack.mulPose(Axis.ZP.rotationDegrees(pose.itemRotZ));
        }

        VertexConsumer solidBuffer = bufferSource.getBuffer(RenderType.solid());
        Matrix4f mat = poseStack.last().pose();
        float scale = 1.0f / 32.0f;

        for (VoxelQuad quad : quads) {
            int light = quad.emissive ? 0xF000F0 : (context == ItemDisplayContext.GUI ? 0xF000F0 : packedLight);

            float x1 = (quad.x1 - 16) * scale;
            float y1 = (quad.y1 - 16) * scale;
            float z1 = (quad.z1 - 16) * scale;

            float x2 = (quad.x2 - 16) * scale;
            float y2 = (quad.y2 - 16) * scale;
            float z2 = (quad.z2 - 16) * scale;

            float r = quad.getRed(), g = quad.getGreen(), b = quad.getBlue();
            solidBuffer.vertex(mat, x1, y2, z1).color(r, g, b, 1.0f).uv(0, 0).overlayCoords(packedOverlay).uv2(light).normal(0, 1, 0).endVertex();
            solidBuffer.vertex(mat, x1, y2, z2).color(r, g, b, 1.0f).uv(0, 1).overlayCoords(packedOverlay).uv2(light).normal(0, 1, 0).endVertex();
            solidBuffer.vertex(mat, x2, y2, z2).color(r, g, b, 1.0f).uv(1, 1).overlayCoords(packedOverlay).uv2(light).normal(0, 1, 0).endVertex();
            solidBuffer.vertex(mat, x2, y2, z1).color(r, g, b, 1.0f).uv(1, 0).overlayCoords(packedOverlay).uv2(light).normal(0, 1, 0).endVertex();
        }

        poseStack.popPose();
        return true;
    }
}
