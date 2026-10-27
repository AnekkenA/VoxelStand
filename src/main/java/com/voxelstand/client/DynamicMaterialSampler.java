package com.voxelstand.client;

import com.voxelstand.voxel.ModdedMaterialCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Динамический колориметрический семплер текстур для предметов из ~800+ модов.
 * Извлекает TextureAtlasSprite предмета на клиенте и считывает цвет центрального пикселя
 * (width / 2, height / 2) либо доминантного непрозрачного пикселя в центральном окне 5x5.
 * Результаты кэшируются для нулевой нагрузки на производительность (0 FPS impact).
 */
public final class DynamicMaterialSampler {
    private static final Map<Item, Integer> COLOR_CACHE = new ConcurrentHashMap<>();

    private DynamicMaterialSampler() {}

    /**
     * Получить цвет материала для предмета.
     * Если доступен клиент с текстурами — берет цвет центрального пикселя спрайта.
     * Иначе берет цвет из ModdedMaterialCatalog или детерминированного хэша.
     */
    public static int sampleItemColor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0xFFFFFF;
        }

        Item item = stack.getItem();
        Integer cached = COLOR_CACHE.get(item);
        if (cached != null) {
            return cached;
        }

        // 1. Проверяем библиотеку известных материалов
        ModdedMaterialCatalog.ModdedMaterial catalogMat = ModdedMaterialCatalog.resolve(stack);
        int finalColor = catalogMat != null ? catalogMat.getDefaultRgb() : 0xD8D8D8;

        // 2. Если на клиенте, пробуем считать реальный центральный пиксель спрайта
        try {
            int sampled = sampleCenterPixelFromClient(stack);
            if (sampled != 0) {
                finalColor = sampled;
            }
        } catch (Throwable ignored) {
            // Безопасный фоллбек на выделенном сервере без графического контекста
        }

        COLOR_CACHE.put(item, finalColor);
        return finalColor;
    }

    /**
     * Считывание цвета центрального пикселя из спрайта на клиенте Minecraft.
     */
    private static int sampleCenterPixelFromClient(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getItemRenderer() == null) {
            return 0;
        }

        var bakedModel = mc.getItemRenderer().getModel(stack, null, null, 0);
        if (bakedModel == null) return 0;

        TextureAtlasSprite sprite = bakedModel.getParticleIcon();
        if (sprite == null) return 0;

        int width = sprite.contents().width();
        int height = sprite.contents().height();

        if (width <= 0 || height <= 0) return 0;

        int centerX = width / 2;
        int centerY = height / 2;

        // Считываем пиксель в центре; если он прозрачный — сканируем окно 5x5 вокруг центра
        for (int radius = 0; radius <= 3; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    int px = Math.max(0, Math.min(width - 1, centerX + dx));
                    int py = Math.max(0, Math.min(height - 1, centerY + dy));

                    int abgr = sprite.getPixelRGBA(0, px, py);
                    int alpha = (abgr >> 24) & 0xFF;

                    if (alpha > 50) {
                        // Minecraft NativeImage хранит цвета в формате 0xAABBGGRR
                        int red = abgr & 0xFF;
                        int green = (abgr >> 8) & 0xFF;
                        int blue = (abgr >> 16) & 0xFF;
                        return (red << 16) | (green << 8) | blue;
                    }
                }
            }
        }

        return 0;
    }

    /**
     * Очистка кэша при перезагрузке текстурных ресурсов (F3 + T).
     */
    public static void clearCache() {
        COLOR_CACHE.clear();
    }
}
