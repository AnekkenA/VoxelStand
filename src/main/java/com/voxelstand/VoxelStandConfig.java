package com.voxelstand;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Конфигурация мода Voxel Stand (файл config/voxelstand-common.toml).
 * Позволяет одним изменением true/false включать и отключать любые механики.
 */
public class VoxelStandConfig {
    public static final VoxelStandConfig CONFIG;
    public static final ModConfigSpec SPEC;

    static {
        Pair<VoxelStandConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(VoxelStandConfig::new);
        CONFIG = pair.getLeft();
        SPEC = pair.getRight();
    }

    /** Включить расчет и перезапись характеристик (урон, вес, броня). Если false — ковка всегда чисто косметическая. */
    public final ModConfigSpec.BooleanValue enableStatCalculation;

    /** Авто-генерация 3D воксельного каркаса прямо из 2D текстуры предмета и развертки брони. */
    public final ModConfigSpec.BooleanValue enableTextureVoxelization;

    /** Создавать предметы с нуля по умолчанию (пустая сетка вместо готового каркаса). */
    public final ModConfigSpec.BooleanValue defaultSculptFromScratch;

    /** Режим косметики по умолчанию (сохраняет ванильные статы, меняя только внешность). */
    public final ModConfigSpec.BooleanValue defaultCosmeticMode;

    /** Запретить крафт снаряжения выше дерева/кожи на обычном верстаке (железо/алмазы требуют стапель). */
    public final ModConfigSpec.BooleanValue restrictVanillaCrafting;

    /** Начисление ванильных зачарований от материалов (например, кварц = острота). */
    public final ModConfigSpec.BooleanValue enableMaterialEnchantments;

    public VoxelStandConfig(ModConfigSpec.Builder builder) {
        builder.push("mechanics");

        enableStatCalculation = builder
                .comment("Включить расчет и изменение RPG-характеристик (урон, скорость, прочность, броня, вес).",
                         "Если false — стапель работает исключительно как косметический трансмогрифер (внешний вид меняется, ванильные статы остаются прежними).")
                .define("enableStatCalculation", true);

        enableTextureVoxelization = builder
                .comment("Автоматически генерировать воксельную модель по оригинальной 2D текстуре предмета и развертке брони.",
                         "Если false — используется процедурный алгоритмический каркас.")
                .define("enableTextureVoxelization", true);

        defaultSculptFromScratch = builder
                .comment("Начинать моделирование с абсолютно пустой сетки по умолчанию (создание с нуля).",
                         "Если false — создается каркас по текстуре, но игрок может начать с нуля зажав Shift.")
                .define("defaultSculptFromScratch", false);

        defaultCosmeticMode = builder
                .comment("Включать режим косметики по умолчанию при установке предмета на стапель.")
                .define("defaultCosmeticMode", false);

        restrictVanillaCrafting = builder
                .comment("Ограничение крафта: на обычном верстаке можно скрафтить только деревянные инструменты и кожаную броню.",
                         "Всё снаряжение из камня, железа, золота, алмазов и незерита создается только через стапель кузницы.")
                .define("restrictVanillaCrafting", false);

        enableMaterialEnchantments = builder
                .comment("Начислять ванильные зачарования при использовании определенных материалов (кварц = острота и т.д.).",
                         "Отключите (false), если не хотите чтобы предметы получали зачарования на стапеле.")
                .define("enableMaterialEnchantments", false);

        builder.pop();
    }
}
