package com.voxelstand.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Слой рендеринга кастомной воксельной брони на игроке (NeoForge 1.21.1).
 * Рендеринг выполняется через IClientItemExtensions.getHumanoidArmorModel (VoxelArmorHumanoidModel)
 * для полной интеграции в конвейер рендеринга брони Minecraft.
 */
public class CustomArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {

    public CustomArmorLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        // Рендеринг воксельной брони делегирован в getHumanoidArmorModel (VoxelArmorHumanoidModel),
        // что автоматически исключает наложение ванильной 2D текстуры и предотвращает повторный рендеринг.
    }
}
