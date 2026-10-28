package com.voxelstand.client.render;

import com.voxelstand.api.VoxelConstants;
import com.voxelstand.mesh.GreedyMesher;
import com.voxelstand.mesh.VoxelQuad;
import com.voxelstand.voxel.VoxelCompressor;
import com.voxelstand.voxel.VoxelGrid;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Рендерер кастомного воксельного слоя брони на игроке и мобах.
 * Привязывает отрендеренный меш прямо к соответствующим костям HumanoidModel.
 * Использует entityCutoutNoCull, предотвращая выворачивание полигонов при инверсии по Y.
 */
public class CustomArmorLayerRenderer {
    private static final Map<String, List<VoxelQuad>> MESH_CACHE = new HashMap<>();

    public static void renderArmorPiece(PoseStack poseStack, MultiBufferSource bufferSource,
                                        LivingEntity entity, EquipmentSlot slot, ItemStack stack,
                                        HumanoidModel<?> model, int packedLight) {
        if (stack.isEmpty() || !com.voxelstand.api.VoxelDataHelper.hasCustomKey(stack, VoxelConstants.NBT_VOXEL_MODEL)) {
            return;
        }

        CompoundTag modelTag = com.voxelstand.api.VoxelDataHelper.getCustomData(stack).getCompound(VoxelConstants.NBT_VOXEL_MODEL);

        switch (slot) {
            case HEAD -> renderPartToBone(poseStack, bufferSource, modelTag, "head", model.head, 0, -0.25f, 0, packedLight);
            case CHEST -> {
                renderPartToBone(poseStack, bufferSource, modelTag, "body", model.body, 0, 0.375f, 0, packedLight);
                renderPartToBone(poseStack, bufferSource, modelTag, "left_arm", model.leftArm, 0.0625f, 0.25f, 0, packedLight);
                renderPartToBone(poseStack, bufferSource, modelTag, "right_arm", model.rightArm, -0.0625f, 0.25f, 0, packedLight);
            }
            case LEGS -> {
                renderPartToBone(poseStack, bufferSource, modelTag, "pelvis", model.body, 0, 0.65f, 0, packedLight);
                renderPartToBone(poseStack, bufferSource, modelTag, "left_leg", model.leftLeg, 0, 0.35f, 0, packedLight);
                renderPartToBone(poseStack, bufferSource, modelTag, "right_leg", model.rightLeg, 0, 0.35f, 0, packedLight);
            }
            case FEET -> {
                renderPartToBone(poseStack, bufferSource, modelTag, "left_boot", model.leftLeg, 0, 0.60f, 0, packedLight);
                renderPartToBone(poseStack, bufferSource, modelTag, "right_boot", model.rightLeg, 0, 0.60f, 0, packedLight);
            }
        }
    }

    private static void renderPartToBone(PoseStack poseStack, MultiBufferSource bufferSource,
                                         CompoundTag modelTag, String partName, ModelPart bone,
                                         float offX, float offY, float offZ, int packedLight) {
        if (!modelTag.contains(partName)) return;

        poseStack.pushPose();
        bone.translateAndRotate(poseStack);

        poseStack.translate(offX, offY, offZ);
        // Масштаб: 32 вокселя соответствуют 16 пикселям игрока + небольшой зазор
        poseStack.scale(0.56f, -0.56f, 0.56f);
        poseStack.translate(-0.5, -0.5, -0.5);

        byte[] data = modelTag.getByteArray(partName);
        String cacheKey = partName + "_" + java.util.Arrays.hashCode(data);

        List<VoxelQuad> quads = MESH_CACHE.computeIfAbsent(cacheKey, k -> {
            VoxelGrid grid = VoxelCompressor.decompress(data);
            return GreedyMesher.mesh(grid);
        });

        // Entity cutout без culling для правильного отображения фронтальных и тыльных граней
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        Matrix4f mat = poseStack.last().pose();
        float scale = 1.0f / 32.0f;

        for (VoxelQuad quad : quads) {
            int light = quad.emissive ? 0xF000F0 : packedLight;

            float x1 = quad.x1 * scale;
            float y1 = quad.y1 * scale;
            float z1 = quad.z1 * scale;
            float x2 = quad.x2 * scale;
            float y2 = quad.y2 * scale;
            float z2 = quad.z2 * scale;

            float r = quad.getRed(), g = quad.getGreen(), b = quad.getBlue();
            VoxelRenderHelper.renderFace(buffer, mat, x1, y1, z1, x2, y2, z2, quad.direction,
                    r, g, b, 1.0f, light, 0);
        }

        poseStack.popPose();
    }
}
