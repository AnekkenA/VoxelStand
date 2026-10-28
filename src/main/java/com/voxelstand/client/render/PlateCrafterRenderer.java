package com.voxelstand.client.render;

import com.voxelstand.block.entity.PlateCrafterBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class PlateCrafterRenderer implements BlockEntityRenderer<PlateCrafterBlockEntity> {
    public PlateCrafterRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(PlateCrafterBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // Станок нарезки пластин рендерится через полноценную 3D-модель block/plate_crafter.json
        // с единым атласом текстур plate_crafter.png.
    }
}
