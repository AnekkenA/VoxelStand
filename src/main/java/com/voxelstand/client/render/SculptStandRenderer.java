package com.voxelstand.client.render;

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
import org.joml.Matrix4f;

import java.util.List;
import java.util.Map;

public class SculptStandRenderer implements BlockEntityRenderer<SculptStandBlockEntity> {
    public SculptStandRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(SculptStandBlockEntity stand, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.2, 0.5);

        SculptStandBlockEntity.StandMode mode = stand.getMode();

        // 1. Отрисовка базовой деревянной рамы и стапеля
        renderBlacksmithBase(mode, poseStack, bufferSource, packedLight, packedOverlay);

        // 2. Если стойка не пуста — отрисовка ориентиров (соломенный манекен / хват / веревки)
        int guideMode = stand.getGuideMode();
        if (mode != SculptStandBlockEntity.StandMode.EMPTY && guideMode != 2) {
            renderGuides(stand, mode, guideMode, poseStack, bufferSource, packedLight, packedOverlay);
        }

        // 3. Отрисовка воксельных частей модели (жадный меш)
        renderVoxelMeshes(stand, mode, poseStack, bufferSource, packedLight, packedOverlay);

        // 3.1. Отрисовка голограммы-призрака чертежа (прозрачность 0.7f по ТЗ)
        if (stand.hasGhostBlueprint()) {
            renderGhostMeshes(stand, mode, poseStack, bufferSource, packedLight, packedOverlay);
        }

        // 4. Отрисовка индикатора симметрии
        if (stand.isSymmetryMode() && mode != SculptStandBlockEntity.StandMode.EMPTY) {
            renderSymmetryLine(poseStack, bufferSource);
        }

        poseStack.popPose();
    }

    private void renderBlacksmithBase(SculptStandBlockEntity.StandMode mode, PoseStack poseStack,
                                      MultiBufferSource bufferSource, int light, int overlay) {
        VertexConsumer builder = bufferSource.getBuffer(RenderType.solid());
        Matrix4f mat = poseStack.last().pose();

        float rWood = 0.40f, gWood = 0.25f, bWood = 0.15f; // Дуб

        // Основание верстака
        renderBox(builder, mat, -0.35f, -0.2f, -0.35f, 0.35f, 0.0f, 0.35f, rWood, gWood, bWood, 1.0f, light, overlay);

        if (mode == SculptStandBlockEntity.StandMode.WEAPON) {
            // Деревянные тиски для оружия
            renderBox(builder, mat, -0.2f, 0.0f, -0.1f, 0.2f, 0.12f, 0.1f, rWood * 0.8f, gWood * 0.8f, bWood * 0.8f, 1.0f, light, overlay);
            // Зеленая стрелка-указатель направления удара / кромки (ВПЕРЕД)
            renderBox(builder, mat, -0.03f, 0.002f, -0.32f, 0.03f, 0.006f, -0.15f, 0.2f, 0.9f, 0.3f, 1.0f, 0xF000F0, overlay);
            renderBox(builder, mat, -0.07f, 0.002f, -0.25f, -0.03f, 0.006f, -0.21f, 0.2f, 0.9f, 0.3f, 1.0f, 0xF000F0, overlay);
            renderBox(builder, mat, 0.03f, 0.002f, -0.25f, 0.07f, 0.006f, -0.21f, 0.2f, 0.9f, 0.3f, 1.0f, 0xF000F0, overlay);
        } else if (mode != SculptStandBlockEntity.StandMode.EMPTY) {
            // Верхняя Т-образная перекладина для брони
            renderBox(builder, mat, -0.45f, 1.2f, -0.05f, 0.45f, 1.3f, 0.05f, rWood, gWood, bWood, 1.0f, light, overlay);
            // Пеньковые веревки
            float rRope = 0.70f, gRope = 0.60f, bRope = 0.40f;
            renderBox(builder, mat, -0.3f, 0.8f, -0.02f, -0.28f, 1.2f, 0.02f, rRope, gRope, bRope, 1.0f, light, overlay);
            renderBox(builder, mat, 0.28f, 0.8f, -0.02f, 0.3f, 1.2f, 0.02f, rRope, gRope, bRope, 1.0f, light, overlay);
        }
    }

    private void renderGuides(SculptStandBlockEntity stand, SculptStandBlockEntity.StandMode mode, int guideMode, PoseStack poseStack,
                             MultiBufferSource bufferSource, int light, int overlay) {
        VertexConsumer builder = bufferSource.getBuffer(RenderType.solid());
        Matrix4f mat = poseStack.last().pose();

        if (guideMode == 0) {
            // Соломенные манекены
            float rStraw = 0.85f, gStraw = 0.75f, bStraw = 0.45f;
            switch (mode) {
                case WEAPON -> {
                    // Соломенный кулак в месте хвата (Y=6..12 / 32)
                    float halfW = 3.0f / 32.0f;
                    boolean twoHanded = stand.getCurrentPose() != null && stand.getCurrentPose().twoHanded;
                    renderBox(builder, mat, -halfW, 6.0f / 32.0f, -halfW, halfW, 13.0f / 32.0f, halfW, rStraw, gStraw, bStraw, 1.0f, light, overlay);
                    if (twoHanded) {
                        // Второй кулак ниже при двуручном хвате
                        renderBox(builder, mat, -halfW, 0.0f / 32.0f, -halfW, halfW, 6.0f / 32.0f, halfW, rStraw * 0.9f, gStraw * 0.9f, bStraw * 0.9f, 1.0f, light, overlay);
                    }
                }
                case HELMET -> {
                    // Соломенная голова с разметкой глаз
                    renderBox(builder, mat, -0.25f, 0.4f, -0.25f, 0.25f, 0.9f, 0.25f, rStraw, gStraw, bStraw, 1.0f, light, overlay);
                    // Темная линия глаз
                    renderBox(builder, mat, -0.20f, 0.65f, 0.251f, 0.20f, 0.68f, 0.252f, 0.2f, 0.15f, 0.1f, 1.0f, light, overlay);
                }
                case CHESTPLATE -> {
                    // Торс
                    renderBox(builder, mat, -0.25f, 0.2f, -0.12f, 0.25f, 0.8f, 0.12f, rStraw, gStraw, bStraw, 1.0f, light, overlay);
                }
                case LEGGINGS -> {
                    // Таз
                    renderBox(builder, mat, -0.22f, 0.4f, -0.12f, 0.22f, 0.65f, 0.12f, rStraw, gStraw, bStraw, 1.0f, light, overlay);
                }
                case BOOTS -> {
                    // Ступни
                    renderBox(builder, mat, -0.28f, 0.0f, -0.2f, -0.08f, 0.25f, 0.15f, rStraw, gStraw, bStraw, 1.0f, light, overlay);
                    renderBox(builder, mat, 0.08f, 0.0f, -0.2f, 0.28f, 0.25f, 0.15f, rStraw, gStraw, bStraw, 1.0f, light, overlay);
                }
                default -> {}
            }
        } else if (guideMode == 1) {
            // Деревянный каркас (манекен портного)
            float rWood = 0.30f, gWood = 0.20f, bWood = 0.10f;
            renderBox(builder, mat, -0.03f, 0.1f, -0.03f, 0.03f, 0.9f, 0.03f, rWood, gWood, bWood, 1.0f, light, overlay);
            renderBox(builder, mat, -0.25f, 0.7f, -0.02f, 0.25f, 0.75f, 0.02f, rWood, gWood, bWood, 1.0f, light, overlay);
        }
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
        switch (quad.direction) {
            case UP -> {
                builder.addVertex(mat, x1, y2, z1).setColor(r, g, b, alpha).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
                builder.addVertex(mat, x1, y2, z2).setColor(r, g, b, alpha).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
                builder.addVertex(mat, x2, y2, z2).setColor(r, g, b, alpha).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
                builder.addVertex(mat, x2, y2, z1).setColor(r, g, b, alpha).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            }
            case DOWN -> {
                builder.addVertex(mat, x1, y1, z2).setColor(r * 0.6f, g * 0.6f, b * 0.6f, alpha).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
                builder.addVertex(mat, x1, y1, z1).setColor(r * 0.6f, g * 0.6f, b * 0.6f, alpha).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
                builder.addVertex(mat, x2, y1, z1).setColor(r * 0.6f, g * 0.6f, b * 0.6f, alpha).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
                builder.addVertex(mat, x2, y1, z2).setColor(r * 0.6f, g * 0.6f, b * 0.6f, alpha).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            }
            default -> {
                builder.addVertex(mat, x1, y1, z2).setColor(r * 0.8f, g * 0.8f, b * 0.8f, alpha).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
                builder.addVertex(mat, x2, y1, z2).setColor(r * 0.8f, g * 0.8f, b * 0.8f, alpha).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
                builder.addVertex(mat, x2, y2, z2).setColor(r * 0.8f, g * 0.8f, b * 0.8f, alpha).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
                builder.addVertex(mat, x1, y2, z2).setColor(r * 0.8f, g * 0.8f, b * 0.8f, alpha).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            }
        }
    }

    private void renderBox(VertexConsumer builder, Matrix4f mat, float x1, float y1, float z1,
                           float x2, float y2, float z2, float r, float g, float b, float a, int light, int overlay) {
        builder.addVertex(mat, x1, y2, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        builder.addVertex(mat, x1, y2, z2).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        builder.addVertex(mat, x2, y2, z2).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        builder.addVertex(mat, x2, y2, z1).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
    }

    private void renderSymmetryLine(PoseStack poseStack, MultiBufferSource bufferSource) {
        VertexConsumer lines = bufferSource.getBuffer(RenderType.lines());
        Matrix4f mat = poseStack.last().pose();
        lines.addVertex(mat, 0f, 0f, -0.5f).setColor(0.3f, 0.8f, 1.0f, 0.8f).setNormal(0f, 1f, 0f);
        lines.addVertex(mat, 0f, 1.2f, -0.5f).setColor(0.3f, 0.8f, 1.0f, 0.8f).setNormal(0f, 1f, 0f);
    }
}
