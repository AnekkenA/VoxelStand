package com.voxelstand.client;

import com.voxelstand.VoxelStandMod;
import com.voxelstand.api.ItemPoseData;
import com.voxelstand.api.VoxelConstants;
import com.voxelstand.api.VoxelDataHelper;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * Обработчик свободной позы рук игрока (Minecraft 1.21.1 NeoForge).
 * Настраивает углы вращения плеча (ArmPitch, ArmYaw, ArmRoll) и поддержку двуручного хвата.
 */
@EventBusSubscriber(modid = VoxelStandMod.MOD_ID, value = Dist.CLIENT)
public class PlayerPoseHandler {

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        AbstractClientPlayer player = (AbstractClientPlayer) event.getEntity();
        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();

        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (mainHand.isEmpty() || !VoxelDataHelper.hasCustomKey(mainHand, VoxelConstants.NBT_POSE_DATA)) {
            return;
        }

        ItemPoseData pose = ItemPoseData.fromNbt(VoxelDataHelper.getCustomData(mainHand).getCompound(VoxelConstants.NBT_POSE_DATA));
        boolean isRightHanded = player.getMainArm() == HumanoidArm.RIGHT;

        var arm = isRightHanded ? model.rightArm : model.leftArm;
        var offArm = isRightHanded ? model.leftArm : model.rightArm;

        // 1. Поворот плеча ведущей руки (в радианах)
        arm.xRot += (float) Math.toRadians(pose.armPitch);
        arm.yRot += (float) Math.toRadians(isRightHanded ? pose.armYaw : -pose.armYaw);
        arm.zRot += (float) Math.toRadians(isRightHanded ? pose.armRoll : -pose.armRoll);

        // 2. Двуручный хват: вторая рука подтягивается к рукояти оружия
        if (pose.twoHanded && player.getItemInHand(InteractionHand.OFF_HAND).isEmpty()) {
            offArm.xRot = arm.xRot;
            offArm.yRot = arm.yRot + (isRightHanded ? -0.35f : 0.35f);
            offArm.zRot = (isRightHanded ? -0.15f : 0.15f);
        }
    }
}
