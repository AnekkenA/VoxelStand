package com.voxelstand.client.render;

import com.voxelstand.api.VoxelConstants;
import com.voxelstand.api.VoxelDataHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class VoxelItemOverrides extends ItemOverrides {
    private final ItemOverrides original;

    public VoxelItemOverrides(ItemOverrides original) {
        this.original = original;
    }

    @Override
    public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        BakedModel resolved = original != null ? original.resolve(model, stack, level, entity, seed) : model;
        if (resolved == null) resolved = model;

        if (VoxelDataHelper.hasCustomKey(stack, VoxelConstants.NBT_VOXEL_DATA) ||
            VoxelDataHelper.hasCustomKey(stack, VoxelConstants.NBT_VOXEL_MODEL)) {
            return new DynamicVoxelBakedModel(resolved);
        }
        return resolved;
    }
}
