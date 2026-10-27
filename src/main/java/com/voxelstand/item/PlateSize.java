package com.voxelstand.item;

public enum PlateSize {
    SIZE_16x16("16x16", 16, 16, 1, 2), // 256 вокселей = 2 слитка
    SIZE_16x8("16x8", 16, 8, 1, 1),     // 128 вокселей = 1 слиток
    SIZE_8x8("8x8", 8, 8, 2, 1),        // 2x 64 вокселя = 1 слиток (128 вокс.)
    SIZE_16x4("16x4", 16, 4, 2, 1),     // 2x 64 вокселя = 1 слиток (128 вокс.)
    SIZE_8x4("8x4", 8, 4, 4, 1),        // 4x 32 вокселя = 1 слиток (128 вокс.)
    SIZE_4x4("4x4", 4, 4, 8, 1);        // 8x 16 вокселей = 1 слиток (128 вокс.)

    private final String name;
    private final int width;
    private final int height;
    private final int craftYield;
    private final int ingredientCost;

    PlateSize(String name, int width, int height, int craftYield, int ingredientCost) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.craftYield = craftYield;
        this.ingredientCost = ingredientCost;
    }

    public String getName() {
        return name;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getCraftYield() {
        return craftYield;
    }

    public int getIngredientCost() {
        return ingredientCost;
    }

    public static PlateSize fromString(String name) {
        for (PlateSize size : values()) {
            if (size.name.equalsIgnoreCase(name)) {
                return size;
            }
        }
        return SIZE_8x8;
    }
}
