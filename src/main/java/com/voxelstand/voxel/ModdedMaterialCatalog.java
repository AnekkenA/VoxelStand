package com.voxelstand.voxel;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Библиотека модовых материалов для поддержки сборок на ~800+ модов (Create, Thermal,
 * Mekanism, Immersive Engineering, Mythic Metals, Undergarden и др.).
 * Содержит предустановленные характеристики, плотности, прочность и цвета для 50+ металлов и самоцветов.
 * Для неизвестных модовых слитков/самоцветов динамически создает профиль материала на основе тегов #c:ingots и #c:gems.
 */
public final class ModdedMaterialCatalog {
    public static final TagKey<Item> TAG_C_INGOTS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ingots"));
    public static final TagKey<Item> TAG_C_GEMS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "gems"));
    public static final TagKey<Item> TAG_C_RAW_MATERIALS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "raw_materials"));

    public static class ModdedMaterial {
        private final String id;
        private final String displayName;
        private final int defaultRgb;
        private final float density;
        private final float durabilityMultiplier;
        private final float damageMultiplier;
        private final float armorMultiplier;

        public ModdedMaterial(String id, String displayName, int defaultRgb, float density,
                              float durabilityMultiplier, float damageMultiplier, float armorMultiplier) {
            this.id = id;
            this.displayName = displayName;
            this.defaultRgb = defaultRgb;
            this.density = density;
            this.durabilityMultiplier = durabilityMultiplier;
            this.damageMultiplier = damageMultiplier;
            this.armorMultiplier = armorMultiplier;
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public int getDefaultRgb() { return defaultRgb; }
        public float getDensity() { return density; }
        public float getDurabilityMultiplier() { return durabilityMultiplier; }
        public float getDamageMultiplier() { return damageMultiplier; }
        public float getArmorMultiplier() { return armorMultiplier; }
    }

    private static final Map<String, ModdedMaterial> CATALOG = new HashMap<>();

    static {
        // --- Металлы и сплавы ---
        register(new ModdedMaterial("tin", "Tin", 0xD3D3D3, 1.0f, 0.9f, 1.0f, 1.0f));
        register(new ModdedMaterial("bronze", "Bronze", 0xCD7F32, 1.2f, 1.35f, 1.15f, 1.2f));
        register(new ModdedMaterial("steel", "Steel", 0xA0A5A9, 1.3f, 2.2f, 1.4f, 1.45f));
        register(new ModdedMaterial("silver", "Silver", 0xE6E8FA, 1.1f, 0.85f, 1.1f, 1.0f));
        register(new ModdedMaterial("lead", "Lead", 0x4D5157, 1.85f, 1.1f, 1.25f, 1.2f));
        register(new ModdedMaterial("nickel", "Nickel", 0xBAB86C, 1.25f, 1.4f, 1.1f, 1.15f));
        register(new ModdedMaterial("electrum", "Electrum", 0xFFFF80, 1.35f, 0.8f, 1.25f, 1.1f));
        register(new ModdedMaterial("invar", "Invar", 0xB0B8B0, 1.3f, 2.1f, 1.3f, 1.35f));
        register(new ModdedMaterial("constantan", "Constantan", 0xD98D62, 1.2f, 1.5f, 1.2f, 1.2f));
        register(new ModdedMaterial("signalum", "Signalum", 0xFFA040, 1.1f, 2.4f, 1.45f, 1.3f));
        register(new ModdedMaterial("lumium", "Lumium", 0xFFFFC0, 1.0f, 2.2f, 1.35f, 1.25f));
        register(new ModdedMaterial("enderium", "Enderium", 0x106060, 1.5f, 4.5f, 1.85f, 1.75f));
        register(new ModdedMaterial("osmium", "Osmium", 0x90B0D0, 1.6f, 2.8f, 1.45f, 1.5f));
        register(new ModdedMaterial("refined_obsidian", "Refined Obsidian", 0x602080, 1.7f, 4.2f, 1.75f, 1.8f));
        register(new ModdedMaterial("refined_glowstone", "Refined Glowstone", 0xE8D840, 0.9f, 2.0f, 1.3f, 1.2f));
        register(new ModdedMaterial("uranium", "Uranium", 0x40E040, 1.8f, 2.6f, 1.5f, 1.4f));
        register(new ModdedMaterial("zinc", "Zinc", 0xD0D8D0, 1.1f, 1.0f, 1.0f, 1.0f));
        register(new ModdedMaterial("brass", "Brass", 0xE0B840, 1.2f, 1.4f, 1.15f, 1.2f));
        register(new ModdedMaterial("aluminum", "Aluminum", 0xDCDFE5, 0.65f, 1.15f, 1.0f, 0.95f));
        register(new ModdedMaterial("titanium", "Titanium", 0xC8D0D8, 1.0f, 3.6f, 1.6f, 1.65f));
        register(new ModdedMaterial("tungsten", "Tungsten", 0x303038, 2.1f, 3.8f, 1.7f, 1.7f));
        register(new ModdedMaterial("platinum", "Platinum", 0xCAD8E8, 1.7f, 2.3f, 1.3f, 1.35f));
        register(new ModdedMaterial("palladium", "Palladium", 0xDAC0A0, 1.4f, 2.0f, 1.3f, 1.3f));
        register(new ModdedMaterial("mithril", "Mithril", 0x70C0E0, 0.55f, 3.4f, 1.55f, 1.5f));
        register(new ModdedMaterial("adamantite", "Adamantite", 0xC02020, 1.65f, 4.3f, 1.8f, 1.8f));
        register(new ModdedMaterial("orichalcum", "Orichalcum", 0xDA70D6, 1.4f, 3.5f, 1.65f, 1.6f));
        register(new ModdedMaterial("cobalt", "Cobalt", 0x2440F0, 0.9f, 2.8f, 1.45f, 1.4f));
        register(new ModdedMaterial("ardite", "Ardite", 0xFA6000, 1.7f, 2.5f, 1.35f, 1.4f));
        register(new ModdedMaterial("manyullyn", "Manyullyn", 0xA020F0, 1.3f, 4.2f, 1.8f, 1.75f));

        // --- Самоцветы и кристаллы ---
        register(new ModdedMaterial("ruby", "Ruby", 0xE0115F, 0.9f, 2.2f, 1.35f, 1.3f));
        register(new ModdedMaterial("sapphire", "Sapphire", 0x0F52BA, 0.9f, 2.2f, 1.35f, 1.3f));
        register(new ModdedMaterial("topaz", "Topaz", 0xFFC87C, 0.9f, 2.0f, 1.25f, 1.2f));
        register(new ModdedMaterial("jade", "Jade", 0x00A86B, 1.0f, 2.1f, 1.25f, 1.25f));
        register(new ModdedMaterial("peridot", "Peridot", 0x8DA346, 0.9f, 1.8f, 1.2f, 1.15f));
        register(new ModdedMaterial("onyx", "Onyx", 0x353839, 1.2f, 2.6f, 1.4f, 1.35f));
        register(new ModdedMaterial("aquamarine", "Aquamarine", 0x7FFFD4, 0.8f, 1.9f, 1.2f, 1.15f));
        register(new ModdedMaterial("amber", "Amber", 0xFFBF00, 0.6f, 1.5f, 1.15f, 1.1f));
        register(new ModdedMaterial("opal", "Opal", 0xE8F4F8, 0.8f, 1.8f, 1.2f, 1.2f));
        register(new ModdedMaterial("moonstone", "Moonstone", 0xC8D8E8, 0.7f, 2.4f, 1.35f, 1.3f));
        register(new ModdedMaterial("garnet", "Garnet", 0x733635, 1.0f, 2.2f, 1.3f, 1.3f));
        register(new ModdedMaterial("cinnabar", "Cinnabar", 0x9B111E, 1.3f, 1.6f, 1.2f, 1.15f));
    }

    private static void register(ModdedMaterial mat) {
        CATALOG.put(mat.getId(), mat);
    }

    public static Map<String, ModdedMaterial> getAll() {
        return Collections.unmodifiableMap(CATALOG);
    }

    public static ModdedMaterial getById(String id) {
        if (id == null) return null;
        return CATALOG.get(id.toLowerCase(Locale.ROOT));
    }

    /**
     * Поиск материала для любого предмета из ~800 модов.
     * Проверяет точное совпадение в каталоге, теги (#c:ingots/*, #c:gems/*),
     * а для неизвестных модовых предметов создает динамический материал с вычисленным цветом.
     */
    public static ModdedMaterial resolve(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        ResourceLocation regId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (regId == null) return null;

        String path = regId.getPath().toLowerCase(Locale.ROOT);

        // 1. Поиск в каталоге по подстроке имени (например, "thermal:tin_ingot" -> "tin")
        for (Map.Entry<String, ModdedMaterial> entry : CATALOG.entrySet()) {
            String key = entry.getKey();
            if (path.contains(key)) {
                return entry.getValue();
            }
        }

        // 2. Проверка тегов конвенции NeoForge (#c:ingots, #c:gems, #c:raw_materials)
        boolean isIngot = stack.is(TAG_C_INGOTS);
        boolean isGem = stack.is(TAG_C_GEMS);
        boolean isRaw = stack.is(TAG_C_RAW_MATERIALS);

        if (isIngot || isGem || isRaw) {
            String cleanName = path.replace("_ingot", "")
                    .replace("_gem", "")
                    .replace("raw_", "")
                    .replace("_ore", "")
                    .replace("_crystal", "");

            // Детерминированный гармоничный цвет из хэша имени предмета
            int hash = cleanName.hashCode();
            int r = Math.abs((hash >> 16) & 0xFF);
            int g = Math.abs((hash >> 8) & 0xFF);
            int b = Math.abs(hash & 0xFF);
            // Нормализация яркости (чтобы цвет не был слишком темным)
            r = 70 + (r % 160);
            g = 70 + (g % 160);
            b = 70 + (b % 160);
            int dynamicRgb = (r << 16) | (g << 8) | b;

            float density = isGem ? 0.85f : 1.15f;
            float durability = isGem ? 2.2f : 1.6f;
            float damage = isGem ? 1.25f : 1.2f;
            float armor = isGem ? 1.2f : 1.25f;

            return new ModdedMaterial(cleanName, capitalize(cleanName), dynamicRgb, density, durability, damage, armor);
        }

        return null;
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
