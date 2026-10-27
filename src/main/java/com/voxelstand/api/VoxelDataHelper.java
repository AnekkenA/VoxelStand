package com.voxelstand.api;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

/**
 * Унифицированный адаптер компонентов данных (Data Components) для Minecraft 1.21.1 NeoForge.
 * Предоставляет безопасный доступ к NBT-тегам предметов без риска сбоев совместимости
 * в соответствии с системой Data Components Minecraft 1.20.5+ / 1.21+.
 */
public final class VoxelDataHelper {
    private VoxelDataHelper() {}

    public static CompoundTag getCustomData(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return new CompoundTag();
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null ? customData.copyTag() : new CompoundTag();
    }

    public static CompoundTag getOrCreateCustomData(ItemStack stack) {
        return getCustomData(stack);
    }

    public static boolean hasCustomData(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.has(DataComponents.CUSTOM_DATA);
    }

    public static boolean hasCustomKey(ItemStack stack, String key) {
        if (!hasCustomData(stack)) return false;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null && customData.contains(key);
    }

    public static void setCustomData(ItemStack stack, CompoundTag tag) {
        if (stack == null || stack.isEmpty()) return;
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void updateCustomData(ItemStack stack, Consumer<CompoundTag> consumer) {
        if (stack == null || stack.isEmpty()) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, consumer);
    }
}
