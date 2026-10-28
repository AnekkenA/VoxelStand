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
            if (grid == null || grid.getNonEmptyCount() == 0) continue;

            poseStack.pushPose();

            if (mode == SculptStandBlockEntity.StandMode.WEAPON) {
                // Оружие лежит горизонтально поперек двух опор стола
                poseStack.translate(0.0, 0.16, 0.0);
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90.0f));
                poseStack.translate(0.0, -0.5, 0.0);

                if (stand.getCurrentPose() != null && stand.getCurrentPose().itemRotY != 0.0f) {
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(stand.getCurrentPose().itemRotY));
                }
            }

            // Динамический сдвиг для брони и взрыв-схемы
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

                VoxelRenderHelper.renderFace(solidBuffer, mat, x1, y1, z1, x2, y2, z2, quad.direction,
                        quad.getRed(), quad.getGreen(), quad.getBlue(), 1.0f, quadLight, overlay);
            }

            poseStack.popPose();
        }
    }

    private void applyPartTransform(PoseStack poseStack, String part, float offset) {
        float baseArmShift = 0.35f;
        if (part.contains("left_arm") || part.contains("left_leg") || part.contains("left_boot")) {
            poseStack.translate(-baseArmShift + offset, 0, 0);
        } else if (part.contains("right_arm") || part.contains("right_leg") || part.contains("right_boot")) {
            poseStack.translate(baseArmShift - offset, 0, 0);
        } else if (part.contains("left")) {
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

            if (mode == SculptStandBlockEntity.StandMode.WEAPON) {
                poseStack.translate(0.0, 0.16, 0.0);
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90.0f));
                poseStack.translate(0.0, -0.5, 0.0);
            }

            applyPartTransform(poseStack, part, stand.getPartOffset(part));
            Matrix4f mat = poseStack.last().pose();

            for (VoxelQuad quad : quads) {
                float x1 = (quad.x1 - 16) * scale;
                float y1 = quad.y1 * scale;
                float z1 = (quad.z1 - 16) * scale;

                float x2 = (quad.x2 - 16) * scale;
                float y2 = quad.y2 * scale;
                float z2 = (quad.z2 - 16) * scale;

                VoxelRenderHelper.renderFace(translucentBuffer, mat, x1, y1, z1, x2, y2, z2, quad.direction,
                        quad.getRed(), quad.getGreen(), quad.getBlue(), 0.7f, 0xF000F0, overlay);
            }

            poseStack.popPose();
        }
    }

    private void renderSymmetryLine(PoseStack poseStack, MultiBufferSource bufferSource) {
        VertexConsumer lines = bufferSource.getBuffer(RenderType.lines());
        Matrix4f mat = poseStack.last().pose();
        lines.addVertex(mat, 0f, 0f, -0.5f).setColor(0.3f, 0.8f, 1.0f, 0.8f).setNormal(0f, 1f, 0f);
        lines.addVertex(mat, 0f, 1.2f, -0.5f).setColor(0.3f, 0.8f, 1.0f, 0.8f).setNormal(0f, 1f, 0f);
    }
}
