package com.voxelstand.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Кастомная модель брони NeoForge для отображения объемных воксельных доспехов на игроке и мобах.
 * Заменяет стандартную плоскую 2D текстуру брони Minecraft на воксельный меш.
 */
public class VoxelArmorHumanoidModel extends HumanoidModel<LivingEntity> {
    public static final VoxelArmorHumanoidModel INSTANCE = new VoxelArmorHumanoidModel(createEmptyRoot());

    private ItemStack currentStack = ItemStack.EMPTY;
    private EquipmentSlot currentSlot = EquipmentSlot.CHEST;

    private static ModelPart createEmptyRoot() {
        return LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(0.0f), 0.0f), 64, 32).bakeRoot();
    }

    public VoxelArmorHumanoidModel(ModelPart root) {
        super(root);
    }

    public void setContext(ItemStack stack, EquipmentSlot slot) {
        this.currentStack = stack;
        this.currentSlot = slot;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        // Не вызываем super.renderToBuffer, чтобы стандартная ванильная 2D-текстура не рисовалась поверх вокселей
        if (this.currentStack != null && !this.currentStack.isEmpty()) {
            CustomArmorLayerRenderer.renderArmorModelToBones(poseStack, this.currentStack, this.currentSlot, this, packedLight, packedOverlay);
        }
    }
}
