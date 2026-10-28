package com.voxelstand.voxel;

import java.util.HashMap;
import java.util.Map;

/**
 * 31 уникальный материал для воксельного моделирования + воздух (0).
 * Занимает ровно 5 бит в битовой упаковке (0..31).
 */
public enum VoxelMaterial {
    AIR((byte) 0, "air", 0x000000, false, "air"),
    
    // 1. Ахроматика и металлы
    QUARTZ((byte) 1, "quartz", 0xFFFFFF, false, "minecraft:quartz"),
    BONE((byte) 2, "bone", 0xE3DAC9, false, "minecraft:bone_block"),
    IRON((byte) 3, "iron", 0xD8D8D8, false, "minecraft:iron_ingot"),
    DEEPSLATE((byte) 4, "deepslate", 0x3F3F44, false, "minecraft:deepslate"),
    NETHERITE((byte) 5, "netherite", 0x31292A, false, "minecraft:netherite_scrap"),
    COPPER((byte) 6, "copper", 0xC06B4F, false, "minecraft:copper_ingot"),
    OBSIDIAN((byte) 7, "obsidian", 0x181223, false, "minecraft:obsidian"), // alt: crying_obsidian

    // 2. Древесина и кожа
    LIGHT_WOOD((byte) 8, "light_wood", 0xD7C185, false, "minecraft:stripped_birch_log"), // alt: stripped_bamboo_block
    OAK((byte) 9, "oak", 0xB38E54, false, "minecraft:stripped_oak_log"),
    JUNGLE((byte) 10, "jungle", 0xB88764, false, "minecraft:stripped_jungle_log"),
    DARK_WOOD((byte) 11, "dark_wood", 0x51351D, false, "minecraft:stripped_spruce_log"), // alt: stripped_dark_oak_log
    MANGROVE((byte) 12, "mangrove", 0x763631, false, "minecraft:stripped_mangrove_log"),
    ACACIA((byte) 13, "acacia", 0xBA6336, false, "minecraft:stripped_acacia_log"),
    CHERRY((byte) 14, "cherry", 0xE6A5A8, false, "minecraft:stripped_cherry_log"),
    LEATHER((byte) 15, "leather", 0x8A593C, false, "minecraft:leather"),

    // 3. Незер и Энд
    CRIMSON((byte) 16, "crimson", 0x6E1D2E, false, "minecraft:stripped_crimson_stem"),
    WARPED((byte) 17, "warped", 0x2B6D6D, false, "minecraft:stripped_warped_stem"),
    PURPUR((byte) 18, "purpur", 0xA778A7, false, "minecraft:purpur_block"),
    SHULKER((byte) 19, "shulker", 0x8C678C, false, "minecraft:shulker_shell"),

    // 4. Драгоценности и кристаллы
    DIAMOND((byte) 20, "diamond", 0x4AEDD9, false, "minecraft:diamond"),
    EMERALD((byte) 21, "emerald", 0x17DD62, false, "minecraft:emerald"),
    REDSTONE((byte) 22, "redstone", 0xFF1111, true, "minecraft:redstone"), // Светится!
    LAPIS((byte) 23, "lapis", 0x1E4399, false, "minecraft:lapis_lazuli"),
    AMETHYST((byte) 24, "amethyst", 0xB565D8, true, "minecraft:amethyst_shard"), // Светится!

    // 5. Оружейный камень и морские материалы
    FLINT((byte) 25, "flint", 0x545454, false, "minecraft:flint"), // Кремень (вместо окисленной меди)
    SEA_LANTERN((byte) 26, "sea_lantern", 0xACD9D5, true, "minecraft:sea_lantern"), // Светится!
    DARK_PRISMARINE((byte) 27, "dark_prismarine", 0x335B4D, false, "minecraft:dark_prismarine"),

    // 6. Золото и сияние
    GOLD((byte) 28, "gold", 0xF8D33C, false, "minecraft:gold_ingot"),
    BLAZE((byte) 29, "blaze", 0xFF9900, true, "minecraft:blaze_rod"), // Светится!
    GLOWSTONE((byte) 30, "glowstone", 0xFFE042, true, "minecraft:glowstone"), // Светится!
    ECHO_SHARD((byte) 31, "echo_shard", 0x07434D, true, "minecraft:echo_shard"); // Светится!

    private final byte id;
    private final String name;
    private final int rgb;
    private final boolean emissive;
    private final String primaryIngredient;

    private static final Map<Byte, VoxelMaterial> BY_ID = new HashMap<>();

    static {
        for (VoxelMaterial mat : values()) {
            BY_ID.put(mat.id, mat);
        }
    }

    VoxelMaterial(byte id, String name, int rgb, boolean emissive, String primaryIngredient) {
        this.id = id;
        this.name = name;
        this.rgb = rgb;
        this.emissive = emissive;
        this.primaryIngredient = primaryIngredient;
    }

    public byte getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getRgb() {
        return rgb;
    }

    public float getRed() {
        return ((rgb >> 16) & 0xFF) / 255.0f;
    }

    public float getGreen() {
        return ((rgb >> 8) & 0xFF) / 255.0f;
    }

    public float getBlue() {
        return (rgb & 0xFF) / 255.0f;
    }

    public boolean isEmissive() {
        return emissive;
    }

    public String getPrimaryIngredient() {
        return primaryIngredient;
    }

    public boolean isEmpty() {
        return this == AIR;
    }

    public static VoxelMaterial byId(byte id) {
        return BY_ID.getOrDefault(id, AIR);
    }
}
