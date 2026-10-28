package com.voxelstand.client.render;

import com.voxelstand.mesh.VoxelFaceDirection;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.Blocks;
import org.joml.Matrix4f;

public final class VoxelRenderHelper {
    private VoxelRenderHelper() {}

    public static TextureAtlasSprite getWhiteSprite() {
        return Minecraft.getInstance().getBlockRenderer().getBlockModelShaper()
                .getParticleIcon(Blocks.WHITE_CONCRETE.defaultBlockState());
    }

    public static void renderFace(VertexConsumer builder, Matrix4f mat,
                                  float x1, float y1, float z1,
                                  float x2, float y2, float z2,
                                  VoxelFaceDirection dir,
                                  float r, float g, float b, float alpha,
                                  int light, int overlay) {
        TextureAtlasSprite sprite = getWhiteSprite();
        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();

        // Выразительное псевдослучайное микро-затенение по 3D координатам вокселя (эффект пиксель-арта)
        int h = ((int) Math.abs(x1 * 64 + y1 * 128 + z1 * 256)) % 7;
        float noise = switch (h) {
            case 0 -> 0.91f;
            case 1 -> 0.96f;
            case 2 -> 1.00f;
            case 3 -> 1.05f;
            case 4 -> 1.09f;
            case 5 -> 0.94f;
            default -> 1.02f;
        };
        r = Math.min(1.0f, r * noise);
        g = Math.min(1.0f, g * noise);
        b = Math.min(1.0f, b * noise);

        // Фасочный градиент для объема граней (верх ярче, низ глубже)
        float topF = 1.04f;
        float botF = 0.93f;

        switch (dir) {
            case UP -> {
                builder.addVertex(mat, x1, y1, z1).setColor(r, g, b, alpha).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
                builder.addVertex(mat, x1, y1, z2).setColor(r, g, b, alpha).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
                builder.addVertex(mat, x2, y1, z2).setColor(r, g, b, alpha).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
                builder.addVertex(mat, x2, y1, z1).setColor(r, g, b, alpha).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            }
            case DOWN -> {
                float dr = r * 0.58f, dg = g * 0.58f, db = b * 0.58f;
                builder.addVertex(mat, x1, y1, z2).setColor(dr, dg, db, alpha).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
                builder.addVertex(mat, x1, y1, z1).setColor(dr, dg, db, alpha).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
                builder.addVertex(mat, x2, y1, z1).setColor(dr, dg, db, alpha).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
                builder.addVertex(mat, x2, y1, z2).setColor(dr, dg, db, alpha).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            }
            case NORTH -> {
                float nr = r * 0.82f, ng = g * 0.82f, nb = b * 0.82f;
                float rTop = Math.min(1.0f, nr * topF), gTop = Math.min(1.0f, ng * topF), bTop = Math.min(1.0f, nb * topF);
                float rBot = nr * botF, gBot = ng * botF, bBot = nb * botF;
                builder.addVertex(mat, x2, y1, z1).setColor(rBot, gBot, bBot, alpha).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
                builder.addVertex(mat, x1, y1, z1).setColor(rBot, gBot, bBot, alpha).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
                builder.addVertex(mat, x1, y2, z1).setColor(rTop, gTop, bTop, alpha).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
                builder.addVertex(mat, x2, y2, z1).setColor(rTop, gTop, bTop, alpha).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
            }
            case SOUTH -> {
                float sr = r * 0.82f, sg = g * 0.82f, sb = b * 0.82f;
                float rTop = Math.min(1.0f, sr * topF), gTop = Math.min(1.0f, sg * topF), bTop = Math.min(1.0f, sb * topF);
                float rBot = sr * botF, gBot = sg * botF, bBot = sb * botF;
                builder.addVertex(mat, x1, y1, z1).setColor(rBot, gBot, bBot, alpha).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
                builder.addVertex(mat, x2, y1, z1).setColor(rBot, gBot, bBot, alpha).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
                builder.addVertex(mat, x2, y2, z1).setColor(rTop, gTop, bTop, alpha).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
                builder.addVertex(mat, x1, y2, z1).setColor(rTop, gTop, bTop, alpha).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            }
            case WEST -> {
                float wr = r * 0.72f, wg = g * 0.72f, wb = b * 0.72f;
                float rTop = Math.min(1.0f, wr * topF), gTop = Math.min(1.0f, wg * topF), bTop = Math.min(1.0f, wb * topF);
                float rBot = wr * botF, gBot = wg * botF, bBot = wb * botF;
                builder.addVertex(mat, x1, y1, z1).setColor(rBot, gBot, bBot, alpha).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
                builder.addVertex(mat, x1, y1, z2).setColor(rBot, gBot, bBot, alpha).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
                builder.addVertex(mat, x1, y2, z2).setColor(rTop, gTop, bTop, alpha).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
                builder.addVertex(mat, x1, y2, z1).setColor(rTop, gTop, bTop, alpha).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
            }
            case EAST -> {
                float er = r * 0.72f, eg = g * 0.72f, eb = b * 0.72f;
                float rTop = Math.min(1.0f, er * topF), gTop = Math.min(1.0f, eg * topF), bTop = Math.min(1.0f, eb * topF);
                float rBot = er * botF, gBot = eg * botF, bBot = eb * botF;
                builder.addVertex(mat, x1, y1, z2).setColor(rBot, gBot, bBot, alpha).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
                builder.addVertex(mat, x1, y1, z1).setColor(rBot, gBot, bBot, alpha).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
                builder.addVertex(mat, x1, y2, z1).setColor(rTop, gTop, bTop, alpha).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
                builder.addVertex(mat, x1, y2, z2).setColor(rTop, gTop, bTop, alpha).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
            }
        }
    }
}
