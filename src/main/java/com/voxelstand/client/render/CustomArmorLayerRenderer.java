package com.voxelstand.client.render;

import com.voxelstand.api.VoxelConstants;
import com.voxelstand.mesh.GreedyMesher;
import com.voxelstand.mesh.VoxelQuad;
import com.voxelstand.voxel.VoxelCompressor;
import com.voxelstand.voxel.VoxelGrid;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
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
 * Синхронизирован со всеми анимациями игрока (бег, приседание, плавание, удары).
 */
public class CustomArmorLayerRenderer {
    private static final Map<String, List<VoxelQuad>> MESH_CACHE = new HashMap<>();

    public static void renderArmorModelToBones(PoseStack poseStack, ItemStack stack, EquipmentSlot slot,
                                               HumanoidModel<?> model, int packedLight, int packedOverlay) {
        if (stack.isEmpty() || !com.voxelstand.api.VoxelDataHelper.hasCustomKey(stack, VoxelConstants.NBT_VOXEL_MODEL)) {
            return;
        }

        CompoundTag modelTag = com.voxelstand.api.VoxelDataHelper.getCustomData(stack).getCompound(VoxelConstants.NBT_VOXEL_MODEL);
        MultiBufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        switch (slot) {
            case HEAD -> renderPartToBone(poseStack, bufferSource, modelTag, "head", model.head, 0, 0, 0, -0.5f, packedLight, packedOverlay);
            case CHEST -> {
                renderPartToBone(poseStack, bufferSource, modelTag, "body", model.body, 0, 0, 0, -0.875f, packedLight, packedOverlay);
                renderPartToBone(poseStack, bufferSource, modelTag, "left_arm", model.leftArm, 0.0625f, 0, 0, -0.75f, packedLight, packedOverlay);
                renderPartToBone(poseStack, bufferSource, modelTag, "right_arm", model.rightArm, -0.0625f, 0, 0, -0.75f, packedLight, packedOverlay);
            }
            case LEGS -> {
                renderPartToBone(poseStack, bufferSource, modelTag, "pelvis", model.body, 0, 0, 0, -0.75f, packedLight, packedOverlay);
                renderPartToBone(poseStack, bufferSource, modelTag, "left_leg", model.leftLeg, 0, 0, 0, -0.5f, packedLight, packedOverlay);
                renderPartToBone(poseStack, bufferSource, modelTag, "right_leg", model.rightLeg, 0, 0, 0, -0.5f, packedLight, packedOverlay);
            }
            case FEET -> {
                renderPartToBone(poseStack, bufferSource, modelTag, "left_boot", model.leftLeg, 0, 0, 0, -0.75f, packedLight, packedOverlay);
                renderPartToBone(poseStack, bufferSource, modelTag, "right_boot", model.rightLeg, 0, 0, 0, -0.75f, packedLight, packedOverlay);
            }
        }
    }

    public static void renderArmorPiece(PoseStack poseStack, MultiBufferSource bufferSource,
                                        LivingEntity entity, EquipmentSlot slot, ItemStack stack,
                                        HumanoidModel<?> model, int packedLight) {
        // Делегирует в renderArmorModelToBones для обратной совместимости
        renderArmorModelToBones(poseStack, stack, slot, model, packedLight, 0);
    }

    private static void renderPartToBone(PoseStack poseStack, MultiBufferSource bufferSource,
                                         CompoundTag modelTag, String partName, ModelPart bone,
                                         float offX, float offY, float offZ, float gridOffsetY,
                                         int packedLight, int packedOverlay) {
        if (!modelTag.contains(partName)) return;

        poseStack.pushPose();
        bone.translateAndRotate(poseStack);

        poseStack.translate(offX, offY, offZ);
        // Масштаб: 32 вокселя соответствуют 16 пикселям игрока + небольшой зазор
        float s = 0.53f;
        poseStack.scale(s, -s, s);
        poseStack.translate(-0.5f, gridOffsetY, -0.5f);

        byte[] data = modelTag.getByteArray(partName);
        String cacheKey = partName + "_" + java.util.Arrays.hashCode(data);

        List<VoxelQuad> quads = MESH_CACHE.computeIfAbsent(cacheKey, k -> {
            VoxelGrid grid = VoxelCompressor.decompress(data);
            return GreedyMesher.mesh(grid);
        });

        // Entity cutout без culling для правильного отображения граней
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
                    r, g, b, 1.0f, light, packedOverlay);
        }

        poseStack.popPose();
    }
}
