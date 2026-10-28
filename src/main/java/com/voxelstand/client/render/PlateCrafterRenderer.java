package com.voxelstand.client.render;

import com.voxelstand.block.entity.PlateCrafterBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class PlateCrafterRenderer implements BlockEntityRenderer<PlateCrafterBlockEntity> {
    public PlateCrafterRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(PlateCrafterBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();

        // 1. Координаты пивота колеса из Blockbench модели plate_crafter.json: origin = [9, 20, 8]
        float pivotX = 9.0f / 16.0f;
        float pivotY = 20.0f / 16.0f;
        float pivotZ = 8.0f / 16.0f;

        poseStack.translate(pivotX, pivotY, pivotZ);

        // 2. Интерполяция угла поворота вокруг оси X (вдоль станка)
        float angle = Mth.lerp(partialTick, be.prevWheelAngle, be.wheelAngle);
        poseStack.mulPose(Axis.XP.rotationDegrees(angle));

        // 3. Возврат системы координат
        poseStack.translate(-pivotX, -pivotY, -pivotZ);

        // 4. Отрисовка точильного колеса и стальной оси
        VertexConsumer builder = bufferSource.getBuffer(RenderType.solid());
        Matrix4f mat = poseStack.last().pose();

        float rStone = 0.58f, gStone = 0.58f, bStone = 0.60f; // Цвет точильного камня
        float rDark = rStone * 0.85f, gDark = gStone * 0.85f, bDark = bStone * 0.85f;

        // Основное тело точильного диска (центральный блок колеса)
        renderBox(builder, mat, 7.0f / 16.0f, 16.0f / 16.0f, 4.0f / 16.0f, 11.0f / 16.0f, 24.0f / 16.0f, 12.0f / 16.0f, rStone, gStone, bStone, packedLight, packedOverlay);

        // Выступы диска для формирования круглого/восьмигранного профиля
        renderBox(builder, mat, 7.0f / 16.0f, 18.0f / 16.0f, 3.0f / 16.0f, 11.0f / 16.0f, 22.0f / 16.0f, 4.0f / 16.0f, rDark, gDark, bDark, packedLight, packedOverlay);
        renderBox(builder, mat, 7.0f / 16.0f, 18.0f / 16.0f, 12.0f / 16.0f, 11.0f / 16.0f, 22.0f / 16.0f, 13.0f / 16.0f, rDark, gDark, bDark, packedLight, packedOverlay);
        renderBox(builder, mat, 7.0f / 16.0f, 24.0f / 16.0f, 6.0f / 16.0f, 11.0f / 16.0f, 25.0f / 16.0f, 10.0f / 16.0f, rStone, gStone, bStone, packedLight, packedOverlay);
        renderBox(builder, mat, 7.0f / 16.0f, 15.0f / 16.0f, 6.0f / 16.0f, 11.0f / 16.0f, 16.0f / 16.0f, 10.0f / 16.0f, rDark, gDark, bDark, packedLight, packedOverlay);

        // Стальная центральная ось вращения
        float rAxle = 0.35f, gAxle = 0.35f, bAxle = 0.38f;
        renderBox(builder, mat, 6.0f / 16.0f, 19.0f / 16.0f, 7.0f / 16.0f, 12.0f / 16.0f, 21.0f / 16.0f, 9.0f / 16.0f, rAxle, gAxle, bAxle, packedLight, packedOverlay);

        poseStack.popPose();
    }

    private void renderBox(VertexConsumer builder, Matrix4f mat, float x1, float y1, float z1,
                           float x2, float y2, float z2, float r, float g, float b, int light, int overlay) {
        // TOP (+Y)
        builder.addVertex(mat, x1, y2, z1).setColor(r, g, b, 1.0f).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        builder.addVertex(mat, x1, y2, z2).setColor(r, g, b, 1.0f).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        builder.addVertex(mat, x2, y2, z2).setColor(r, g, b, 1.0f).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        builder.addVertex(mat, x2, y2, z1).setColor(r, g, b, 1.0f).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);

        // BOTTOM (-Y)
        float rb = r * 0.6f, gb = g * 0.6f, bb = b * 0.6f;
        builder.addVertex(mat, x1, y1, z2).setColor(rb, gb, bb, 1.0f).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
        builder.addVertex(mat, x1, y1, z1).setColor(rb, gb, bb, 1.0f).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
        builder.addVertex(mat, x2, y1, z1).setColor(rb, gb, bb, 1.0f).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
        builder.addVertex(mat, x2, y1, z2).setColor(rb, gb, bb, 1.0f).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);

        // SIDES
        float rs = r * 0.8f, gs = g * 0.8f, bs = b * 0.8f;
        // NORTH (-Z)
        builder.addVertex(mat, x2, y1, z1).setColor(rs, gs, bs, 1.0f).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        builder.addVertex(mat, x1, y1, z1).setColor(rs, gs, bs, 1.0f).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        builder.addVertex(mat, x1, y2, z1).setColor(rs, gs, bs, 1.0f).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        builder.addVertex(mat, x2, y2, z1).setColor(rs, gs, bs, 1.0f).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);

        // SOUTH (+Z)
        builder.addVertex(mat, x1, y1, z2).setColor(rs, gs, bs, 1.0f).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        builder.addVertex(mat, x2, y1, z2).setColor(rs, gs, bs, 1.0f).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        builder.addVertex(mat, x2, y2, z2).setColor(rs, gs, bs, 1.0f).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        builder.addVertex(mat, x1, y2, z2).setColor(rs, gs, bs, 1.0f).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);

        // WEST (-X)
        builder.addVertex(mat, x1, y1, z1).setColor(rs, gs, bs, 1.0f).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
        builder.addVertex(mat, x1, y1, z2).setColor(rs, gs, bs, 1.0f).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
        builder.addVertex(mat, x1, y2, z2).setColor(rs, gs, bs, 1.0f).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
        builder.addVertex(mat, x1, y2, z1).setColor(rs, gs, bs, 1.0f).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);

        // EAST (+X)
        builder.addVertex(mat, x2, y1, z2).setColor(rs, gs, bs, 1.0f).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
        builder.addVertex(mat, x2, y1, z1).setColor(rs, gs, bs, 1.0f).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
        builder.addVertex(mat, x2, y2, z1).setColor(rs, gs, bs, 1.0f).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
        builder.addVertex(mat, x2, y2, z2).setColor(rs, gs, bs, 1.0f).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
    }
}
