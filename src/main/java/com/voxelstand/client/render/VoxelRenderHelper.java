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
        renderFace(builder, mat, x1, y1, z1, x2, y2, z2, dir, r, g, b, alpha, light, overlay, false);
    }

    public static void renderFace(VertexConsumer builder, Matrix4f mat,
                                  float x1, float y1, float z1,
                                  float x2, float y2, float z2,
                                  VoxelFaceDirection dir,
                                  float r, float g, float b, float alpha,
                                  int light, int overlay, boolean isGui) {
        TextureAtlasSprite sprite = getWhiteSprite();
        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();

        float shade = 1.0f;
        if (!isGui) {
            shade = switch (dir) {
                case UP -> 1.0f;
                case NORTH, SOUTH -> 0.96f;
                case EAST, WEST -> 0.90f;
                case DOWN -> 0.78f;
            };
        }

        float cr = Math.min(1.0f, r * shade);
        float cg = Math.min(1.0f, g * shade);
        float cb = Math.min(1.0f, b * shade);

        switch (dir) {
            case UP -> {
                builder.addVertex(mat, x1, y1, z1).setColor(cr, cg, cb, alpha).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
                builder.addVertex(mat, x1, y1, z2).setColor(cr, cg, cb, alpha).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
                builder.addVertex(mat, x2, y1, z2).setColor(cr, cg, cb, alpha).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
                builder.addVertex(mat, x2, y1, z1).setColor(cr, cg, cb, alpha).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            }
            case DOWN -> {
                builder.addVertex(mat, x1, y1, z2).setColor(cr, cg, cb, alpha).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
                builder.addVertex(mat, x1, y1, z1).setColor(cr, cg, cb, alpha).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
                builder.addVertex(mat, x2, y1, z1).setColor(cr, cg, cb, alpha).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
                builder.addVertex(mat, x2, y1, z2).setColor(cr, cg, cb, alpha).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            }
            case NORTH -> {
                builder.addVertex(mat, x2, y1, z1).setColor(cr, cg, cb, alpha).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
                builder.addVertex(mat, x1, y1, z1).setColor(cr, cg, cb, alpha).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
                builder.addVertex(mat, x1, y2, z1).setColor(cr, cg, cb, alpha).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
                builder.addVertex(mat, x2, y2, z1).setColor(cr, cg, cb, alpha).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
            }
            case SOUTH -> {
                builder.addVertex(mat, x1, y1, z1).setColor(cr, cg, cb, alpha).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
                builder.addVertex(mat, x2, y1, z1).setColor(cr, cg, cb, alpha).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
                builder.addVertex(mat, x2, y2, z1).setColor(cr, cg, cb, alpha).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
                builder.addVertex(mat, x1, y2, z1).setColor(cr, cg, cb, alpha).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            }
            case WEST -> {
                builder.addVertex(mat, x1, y1, z1).setColor(cr, cg, cb, alpha).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
                builder.addVertex(mat, x1, y1, z2).setColor(cr, cg, cb, alpha).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
                builder.addVertex(mat, x1, y2, z2).setColor(cr, cg, cb, alpha).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
                builder.addVertex(mat, x1, y2, z1).setColor(cr, cg, cb, alpha).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
            }
            case EAST -> {
                builder.addVertex(mat, x1, y1, z2).setColor(cr, cg, cb, alpha).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
                builder.addVertex(mat, x1, y1, z1).setColor(cr, cg, cb, alpha).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
                builder.addVertex(mat, x1, y2, z1).setColor(cr, cg, cb, alpha).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
                builder.addVertex(mat, x1, y2, z2).setColor(cr, cg, cb, alpha).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
            }
        }
    }
}
