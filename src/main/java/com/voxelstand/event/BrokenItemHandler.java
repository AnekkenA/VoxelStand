package com.voxelstand.event;

import com.voxelstand.VoxelStandMod;
import com.voxelstand.api.VoxelDataHelper;
import com.voxelstand.stats.ItemStatData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;

@EventBusSubscriber(modid = VoxelStandMod.MOD_ID)
public class BrokenItemHandler {

    @SubscribeEvent
    public static void onDestroyItem(PlayerDestroyItemEvent event) {
        ItemStack stack = event.getOriginal();
        if (VoxelDataHelper.hasCustomKey(stack, "CustomSmithStats")) {
            ItemStatData stats = ItemStatData.fromNBT(VoxelDataHelper.getCustomData(stack).getCompound("CustomSmithStats"));

            // Предмет не исчезает! Переходит в статус Broken
            stats.isBroken = true;
            ItemStack brokenItem = stack.copy();
            brokenItem.setDamageValue(brokenItem.getMaxDamage() - 1);
            VoxelDataHelper.updateCustomData(brokenItem, tag -> tag.put("CustomSmithStats", stats.toNBT()));

            if (event.getEntity() instanceof ServerPlayer player) {
                player.getInventory().add(brokenItem);
                player.displayClientMessage(Component.literal("§c[!] Ваш предмет сломан, но не уничтожен! Почините его на наковальне."), false);
                player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0f, 0.8f);
            }
        }
    }
}
