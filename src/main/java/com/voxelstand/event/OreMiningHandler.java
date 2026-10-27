package com.voxelstand.event;

import com.voxelstand.VoxelStandMod;
import com.voxelstand.api.VoxelDataHelper;
import com.voxelstand.stats.ItemStatData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = VoxelStandMod.MOD_ID)
public class OreMiningHandler {

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player == null || player.isCreative()) return;

        ItemStack tool = player.getMainHandItem();
        if (VoxelDataHelper.hasCustomKey(tool, "CustomSmithStats")) {
            ItemStatData stats = ItemStatData.fromNBT(VoxelDataHelper.getCustomData(tool).getCompound("CustomSmithStats"));
            if (stats.isBroken) {
                event.setCanceled(true);
                player.displayClientMessage(Component.literal("§cИнструмент сломан! Почините его на наковальне."), true);
            }
        }
    }
}
