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

        // Тонкое микро-затенение по координатам вокселя (эффект пиксель-арта Minecraft)
        int hash = ((int)Math.abs(x1 * 64 + y1 * 128 + z1 * 256)) & 3;
        float shade = switch (hash) {
            case 0 -> 0.93f;
            case 1 -> 1.00f;
            case 2 -> 1.06f;
            default -> 0.97f;
        };
        r = Math.min(1.0f, r * shade);
        g = Math.min(1.0f, g * shade);
        b = Math.min(1.0f, b * shade);

        switch (dir) {
            case UP -> {
                builder.addVertex(mat, x1, y1, z1).setColor(r, g, b, alpha).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
                builder.addVertex(mat, x1, y1, z2).setColor(r, g, b, alpha).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
                builder.addVertex(mat, x2, y1, z2).setColor(r, g, b, alpha).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
                builder.addVertex(mat, x2, y1, z1).setColor(r, g, b, alpha).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            }
            case DOWN -> {
                float dr = r * 0.6f, dg = g * 0.6f, db = b * 0.6f;
                builder.addVertex(mat, x1, y1, z2).setColor(dr, dg, db, alpha).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
                builder.addVertex(mat, x1, y1, z1).setColor(dr, dg, db, alpha).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
                builder.addVertex(mat, x2, y1, z1).setColor(dr, dg, db, alpha).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
                builder.addVertex(mat, x2, y1, z2).setColor(dr, dg, db, alpha).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            }
            case NORTH -> {
                float nr = r * 0.8f, ng = g * 0.8f, nb = b * 0.8f;
                builder.addVertex(mat, x2, y1, z1).setColor(nr, ng, nb, alpha).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
                builder.addVertex(mat, x1, y1, z1).setColor(nr, ng, nb, alpha).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
                builder.addVertex(mat, x1, y2, z1).setColor(nr, ng, nb, alpha).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
                builder.addVertex(mat, x2, y2, z1).setColor(nr, ng, nb, alpha).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
            }
            case SOUTH -> {
                float sr = r * 0.8f, sg = g * 0.8f, sb = b * 0.8f;
                builder.addVertex(mat, x1, y1, z1).setColor(sr, sg, sb, alpha).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
                builder.addVertex(mat, x2, y1, z1).setColor(sr, sg, sb, alpha).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
                builder.addVertex(mat, x2, y2, z1).setColor(sr, sg, sb, alpha).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
                builder.addVertex(mat, x1, y2, z1).setColor(sr, sg, sb, alpha).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            }
            case WEST -> {
                float wr = r * 0.7f, wg = g * 0.7f, wb = b * 0.7f;
                builder.addVertex(mat, x1, y1, z1).setColor(wr, wg, wb, alpha).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
                builder.addVertex(mat, x1, y1, z2).setColor(wr, wg, wb, alpha).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
                builder.addVertex(mat, x1, y2, z2).setColor(wr, wg, wb, alpha).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
                builder.addVertex(mat, x1, y2, z1).setColor(wr, wg, wb, alpha).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
            }
            case EAST -> {
                float er = r * 0.7f, eg = g * 0.7f, eb = b * 0.7f;
                builder.addVertex(mat, x1, y1, z2).setColor(er, eg, eb, alpha).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
                builder.addVertex(mat, x1, y1, z1).setColor(er, eg, eb, alpha).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
                builder.addVertex(mat, x1, y2, z1).setColor(er, eg, eb, alpha).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
                builder.addVertex(mat, x1, y2, z2).setColor(er, eg, eb, alpha).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
            }
        }
    }
}
