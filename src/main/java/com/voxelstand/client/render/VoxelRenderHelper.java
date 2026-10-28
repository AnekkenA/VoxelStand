package com.voxelstand.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.Blocks;

public final class VoxelRenderHelper {
    private VoxelRenderHelper() {}

    public static TextureAtlasSprite getWhiteSprite() {
        return Minecraft.getInstance().getBlockRenderer().getBlockModelShaper()
                .getParticleIcon(Blocks.WHITE_CONCRETE.defaultBlockState());
    }
}
