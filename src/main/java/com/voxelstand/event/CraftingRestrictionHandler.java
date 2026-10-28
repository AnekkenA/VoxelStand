package com.voxelstand.event;

import com.voxelstand.VoxelStandConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Обработчик ограничения ванильного крафта.
 * Активируется только если в config/voxelstand-common.toml включено restrictVanillaCrafting = true.
 * Разрешает на обычном верстаке крафтить только дерево и кожу.
 * Все металлы и драгоценности (железо, золото, алмазы, незерит) требуют ковки на стапеле.
 */
public class CraftingRestrictionHandler {

    @SubscribeEvent
    public void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!VoxelStandConfig.CONFIG.restrictVanillaCrafting.get()) {
            return;
        }

        ItemStack crafted = event.getCrafting();
        if (crafted.isEmpty()) return;

        Player player = event.getEntity();
        if (player == null || player.isCreative()) return;

        boolean isRestricted = false;

        // Проверка инструментов
        if (crafted.getItem() instanceof TieredItem tiered) {
            if (tiered.getTier() != Tiers.WOOD) {
                isRestricted = true;
            }
        }

        // Проверка брони
        if (crafted.getItem() instanceof ArmorItem armor) {
            if (armor.getMaterial() != ArmorMaterials.LEATHER) {
                isRestricted = true;
            }
        }

        if (isRestricted) {
            player.displayClientMessage(
                    Component.literal("§c[Кузница] Снаряжение выше дерева и кожи требует ковки на стапеле! (Ограничение конфига)"),
                    true
            );
        }
    }
}
