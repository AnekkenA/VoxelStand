package com.voxelstand.item;

import java.util.Random;

/**
 * Градация качества кузнечных пластин.
 * Влияет на итоговые характеристики скованного изделия (включая броню и твердость).
 */
public enum PlateQuality {
    NORMAL("normal", 1.00f, "§7", "Обычная", "Стандартная прокатанная пластина без дополнительной обработки."),
    STURDY("sturdy", 1.15f, "§b", "Крепкая", "Закалена под прессом. Увеличивает характеристики изделия на +15% (урон, прочность, броню, твердость)."),
    PURE("pure", 1.30f, "§6", "Чистая", "Глубоко очищена от примесей. Увеличивает характеристики изделия на +30% (урон, прочность, броню, твердость).");

    private final String id;
    private final float multiplier;
    private final String colorCode;
    private final String displayName;
    private final String description;

    PlateQuality(String id, float multiplier, String colorCode, String displayName, String description) {
        this.id = id;
        this.multiplier = multiplier;
        this.colorCode = colorCode;
        this.displayName = displayName;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public float getMultiplier() {
        return multiplier;
    }

    public String getColorCode() {
        return colorCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static PlateQuality fromString(String name) {
        if (name == null || name.isEmpty()) return NORMAL;
        for (PlateQuality q : values()) {
            if (q.name().equalsIgnoreCase(name) || q.id.equalsIgnoreCase(name)) {
                return q;
            }
        }
        return NORMAL;
    }

    /**
     * Определение качества пластины при осмотре подзорной трубой:
     * - Обычная: 75% (0.00 .. 0.7499)
     * - Крепкая: 20% (0.75 .. 0.9499)
     * - Чистая:   5% (0.95 .. 1.0000)
     */
    public static PlateQuality rollFromSpyglass(Random random) {
        double roll = random.nextDouble();
        if (roll < 0.75) {
            return NORMAL;
        } else if (roll < 0.95) {
            return STURDY;
        } else {
            return PURE;
        }
    }
}
