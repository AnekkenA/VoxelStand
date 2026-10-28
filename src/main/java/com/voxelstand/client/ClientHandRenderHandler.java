package com.voxelstand.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.voxelstand.VoxelStandMod;
import com.voxelstand.api.VoxelConstants;
import com.voxelstand.api.VoxelDataHelper;
import com.voxelstand.client.render.CustomItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * Перехватчик рендера рук игрока для кастомных 3D воксельных предметов от первого лица.
 */
@EventBusSubscriber(modid = VoxelStandMod.MOD_ID, value = Dist.CLIENT)
public class ClientHandRenderHandler {

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        if (VoxelDataHelper.hasCustomKey(stack, VoxelConstants.NBT_VOXEL_DATA) ||
            VoxelDataHelper.hasCustomKey(stack, VoxelConstants.NBT_VOXEL_MODEL)) {

            ItemDisplayContext context = event.getHand() == InteractionHand.MAIN_HAND ?
                    ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;

            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();

            boolean rendered = CustomItemRenderer.renderCustomItem(
                    stack, context, poseStack, event.getMultiBufferSource(), event.getPackedLight(), OverlayTexture.NO_OVERLAY
            );

            poseStack.popPose();

            if (rendered) {
                event.setCanceled(true);
            }
        }
    }
}
