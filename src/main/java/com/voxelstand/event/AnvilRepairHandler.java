package com.voxelstand.event;

import com.voxelstand.VoxelStandMod;
import com.voxelstand.api.VoxelDataHelper;
import com.voxelstand.stats.ItemStatData;
import com.voxelstand.voxel.VoxelMaterial;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

@EventBusSubscriber(modid = VoxelStandMod.MOD_ID)
public class AnvilRepairHandler {

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (left.isEmpty()) return;

        boolean isCustomSmith = VoxelDataHelper.hasCustomKey(left, "CustomSmithStats");

        // Если предмет ванильный или из других модов — не вмешиваемся, чтобы избежать конфликтов в 800+ модах
        if (!isCustomSmith) {
            return;
        }

        // Для предметов VoxelStand: починка по доминантному материалу
        CompoundTag statsTag = VoxelDataHelper.getCustomData(left).getCompound("CustomSmithStats");
        ItemStatData stats = ItemStatData.fromNBT(statsTag);

        Item requiredRepairItem = getRepairIngredient(VoxelMaterial.byId(stats.dominantMaterialId));
        if (right.getItem() == requiredRepairItem) {
            ItemStack output = left.copy();
            int currentDamage = output.getDamageValue();
            if (currentDamage > 0 || stats.isBroken) {
                int repairPerItem = Math.max(1, output.getMaxDamage() / 4);
                int itemsToUse = 0;

                while (itemsToUse < right.getCount() && (currentDamage > 0 || stats.isBroken)) {
                    currentDamage = Math.max(0, currentDamage - repairPerItem);
                    stats.isBroken = false;
                    itemsToUse++;
                }

                output.setDamageValue(currentDamage);
                CompoundTag newStatsTag = stats.toNBT();
                VoxelDataHelper.updateCustomData(output, tag -> tag.put("CustomSmithStats", newStatsTag));

                event.setOutput(output);
                event.setMaterialCost(itemsToUse);
                event.setCost(1 + itemsToUse);
            }
        }
    }

    private static Item getRepairIngredient(VoxelMaterial mat) {
        return switch (mat) {
            case COPPER -> Items.COPPER_INGOT;
            case IRON -> Items.IRON_INGOT;
            case GOLD -> Items.GOLD_INGOT;
            case DIAMOND -> Items.DIAMOND;
            case NETHERITE -> Items.NETHERITE_INGOT;
            case OBSIDIAN -> Items.OBSIDIAN;
            case FLINT -> Items.FLINT;
            case DEEPSLATE -> Items.COBBLED_DEEPSLATE;
            case LEATHER -> Items.LEATHER;
            default -> Items.OAK_PLANKS;
        };
    }
}
