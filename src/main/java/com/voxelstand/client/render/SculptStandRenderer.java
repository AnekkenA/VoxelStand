package com.voxelstand.client.render;

import com.voxelstand.block.SculptStandBlock;
import com.voxelstand.block.entity.SculptStandBlockEntity;
import com.voxelstand.mesh.GreedyMesher;
import com.voxelstand.mesh.VoxelQuad;
import com.voxelstand.voxel.VoxelGrid;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Map;

public class SculptStandRenderer implements BlockEntityRenderer<SculptStandBlockEntity> {
    public SculptStandRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(SculptStandBlockEntity stand, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Direction facing = stand.getBlockState().hasProperty(SculptStandBlock.FACING) ?
                stand.getBlockState().getValue(SculptStandBlock.FACING) : Direction.NORTH;

        poseStack.pushPose();
        // Вращение относительно центра опорного блока (0.5, 0, 0.5)
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.translate(-0.5, 0.0, -0.5);

        // Центрирование воксельной сетки над 2-блочной столешницей (X:1.0, Y:0.95, Z:0.5)
        poseStack.translate(1.0, 0.95, 0.5);

        SculptStandBlockEntity.StandMode mode = stand.getMode();

        // 1. Отрисовка воксельных частей модели (жадный меш)
        renderVoxelMeshes(stand, mode, poseStack, bufferSource, packedLight, packedOverlay);

        // 2. Отрисовка голограммы-призрака чертежа (прозрачность 0.7f по ТЗ)
        if (stand.hasGhostBlueprint()) {
            renderGhostMeshes(stand, mode, poseStack, bufferSource, packedLight, packedOverlay);
        }

        // 3. Отрисовка индикатора симметрии
        if (stand.isSymmetryMode() && mode != SculptStandBlockEntity.StandMode.EMPTY) {
            renderSymmetryLine(poseStack, bufferSource);
        }

        poseStack.popPose();
    }

    private void renderVoxelMeshes(SculptStandBlockEntity stand, SculptStandBlockEntity.StandMode mode,
                                   PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay) {
        float scale = 1.0f / 32.0f;

        for (Map.Entry<String, VoxelGrid> entry : stand.getAllGrids().entrySet()) {
            String part = entry.getKey();
            VoxelGrid grid = entry.getValue();
            if (grid.getNonEmptyCount() == 0) continue;

            poseStack.pushPose();

            // Динамический поворот оружия при настройке позы
            if (mode == SculptStandBlockEntity.StandMode.WEAPON && stand.getCurrentPose() != null && stand.getCurrentPose().itemRotY != 0.0f) {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(stand.getCurrentPose().itemRotY));
            }

            // Динамический сдвиг для взрыв-схемы
            applyPartTransform(poseStack, part, stand.getPartOffset(part));

            List<VoxelQuad> quads = GreedyMesher.mesh(grid);
            VertexConsumer solidBuffer = bufferSource.getBuffer(RenderType.solid());
            Matrix4f mat = poseStack.last().pose();

            for (VoxelQuad quad : quads) {
                int quadLight = quad.emissive ? 0xF000F0 : light;

                float x1 = (quad.x1 - 16) * scale;
                float y1 = quad.y1 * scale;
                float z1 = (quad.z1 - 16) * scale;

                float x2 = (quad.x2 - 16) * scale;
                float y2 = quad.y2 * scale;
                float z2 = (quad.z2 - 16) * scale;

                renderFace(solidBuffer, mat, x1, y1, z1, x2, y2, z2, quad, quadLight, overlay, 1.0f);
            }

            poseStack.popPose();
        }
    }

    private void applyPartTransform(PoseStack poseStack, String part, float offset) {
        if (part.contains("left")) {
            poseStack.translate(offset, 0, 0);
        } else if (part.contains("right")) {
            poseStack.translate(-offset, 0, 0);
        }
    }

    private void renderGhostMeshes(SculptStandBlockEntity stand, SculptStandBlockEntity.StandMode mode, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int light, int overlay) {
        VertexConsumer translucentBuffer = bufferSource.getBuffer(RenderType.entityTranslucentCull(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS));
        float scale = 1.0f / 32.0f;

        for (Map.Entry<String, VoxelGrid> entry : stand.getGhostGrids().entrySet()) {
            String part = entry.getKey();
            VoxelGrid grid = entry.getValue();
            if (grid == null || grid.getNonEmptyCount() == 0) continue;

            List<VoxelQuad> quads = GreedyMesher.mesh(grid);
            poseStack.pushPose();
            applyPartTransform(poseStack, part, stand.getPartOffset(part));
            Matrix4f mat = poseStack.last().pose();

            for (VoxelQuad quad : quads) {
                float x1 = (quad.x1 - 16) * scale;
                float y1 = quad.y1 * scale;
                float z1 = (quad.z1 - 16) * scale;

                float x2 = (quad.x2 - 16) * scale;
                float y2 = quad.y2 * scale;
                float z2 = (quad.z2 - 16) * scale;

                // Прозрачность 0.7f по ТЗ, легкое свечение для четкой видимости чертежа
                renderFace(translucentBuffer, mat, x1, y1, z1, x2, y2, z2, quad, 0xF000F0, overlay, 0.7f);
            }

            poseStack.popPose();
        }
    }

    private void renderFace(VertexConsumer builder, Matrix4f mat, float x1, float y1, float z1,
                            float x2, float y2, float z2, VoxelQuad quad, int light, int overlay, float alpha) {
        float r = quad.getRed(), g = quad.getGreen(), b = quad.getBlue();
        TextureAtlasSprite sprite = VoxelRenderHelper.getWhiteSprite();
        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();

        switch (quad.direction) {
            case UP -> {
                builder.addVertex(mat, x1, y2, z1).setColor(r, g, b, alpha).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
                builder.addVertex(mat, x1, y2, z2).setColor(r, g, b, alpha).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
                builder.addVertex(mat, x2, y2, z2).setColor(r, g, b, alpha).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
                builder.addVertex(mat, x2, y2, z1).setColor(r, g, b, alpha).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            }
            case DOWN -> {
                float dr = r * 0.6f, dg = g * 0.6f, db = b * 0.6f;
                builder.addVertex(mat, x1, y1, z2).setColor(dr, dg, db, alpha).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
                builder.addVertex(mat, x1, y1, z1).setColor(dr, dg, db, alpha).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
                builder.addVertex(mat, x2, y1, z1).setColor(dr, dg, db, alpha).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
                builder.addVertex(mat, x2, y1, z2).setColor(dr, dg, db, alpha).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            }
            default -> {
                float sr = r * 0.8f, sg = g * 0.8f, sb = b * 0.8f;
                builder.addVertex(mat, x1, y1, z2).setColor(sr, sg, sb, alpha).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
                builder.addVertex(mat, x2, y1, z2).setColor(sr, sg, sb, alpha).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
                builder.addVertex(mat, x2, y2, z2).setColor(sr, sg, sb, alpha).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
                builder.addVertex(mat, x1, y2, z2).setColor(sr, sg, sb, alpha).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            }
        }
    }

    private void renderSymmetryLine(PoseStack poseStack, MultiBufferSource bufferSource) {
        VertexConsumer lines = bufferSource.getBuffer(RenderType.lines());
        Matrix4f mat = poseStack.last().pose();
        lines.addVertex(mat, 0f, 0f, -0.5f).setColor(0.3f, 0.8f, 1.0f, 0.8f).setNormal(0f, 1f, 0f);
        lines.addVertex(mat, 0f, 1.2f, -0.5f).setColor(0.3f, 0.8f, 1.0f, 0.8f).setNormal(0f, 1f, 0f);
    }
}
