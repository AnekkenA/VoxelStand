package com.voxelstand.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Унифицированный кастомный рендерер предметов для NeoForge 1.21.1.
 * Перехватывает вызовы отрисовки предметов во всех контекстах:
 *  - Инвентарь и хотбар (GUI)
 *  - Руки от 1-го и 3-го лица
 *  - Выброшенные предметы на земле
 *  - Рамки для предметов
 */
public class VoxelItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final VoxelItemRenderer INSTANCE = new VoxelItemRenderer();

    public VoxelItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!CustomItemRenderer.renderCustomItem(stack, context, poseStack, bufferSource, packedLight, packedOverlay)) {
            try {
                IClientItemExtensions.DEFAULT.getCustomRenderer().renderByItem(stack, context, poseStack, bufferSource, packedLight, packedOverlay);
            } catch (Throwable ignored) {
            }
        }
    }
}
