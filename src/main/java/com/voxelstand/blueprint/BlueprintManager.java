package com.voxelstand.blueprint;

import com.voxelstand.api.ItemPoseData;
import com.voxelstand.api.VoxelConstants;
import com.voxelstand.block.entity.SculptStandBlockEntity;
import com.voxelstand.stats.ItemStatData;
import com.voxelstand.stats.StatCalculator;
import com.voxelstand.voxel.VoxelCompressor;
import com.voxelstand.voxel.VoxelGrid;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Менеджер сохранения, загрузки и обмена чертежами воксельных моделей.
 * Файлы хранятся в папке: config/customsmith/blueprints/<имя>.json
 */
public final class BlueprintManager {
    public static final Path BLUEPRINTS_DIR = Paths.get("config", "voxelstand", "blueprints");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private BlueprintManager() {}

    public static void init() {
        try {
            if (!Files.exists(BLUEPRINTS_DIR)) {
                Files.createDirectories(BLUEPRINTS_DIR);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Сохранение текущей модели со стапеля в файл чертежа.
     */
    public static boolean saveBlueprint(String name, SculptStandBlockEntity stand, String author) {
        init();
        if (stand == null || stand.getMode() == SculptStandBlockEntity.StandMode.EMPTY) {
            return false;
        }

        try {
            JsonObject root = new JsonObject();
            root.addProperty("version", 1);
            root.addProperty("name", name);
            root.addProperty("author", author != null ? author : "Unknown");
            root.addProperty("mode", stand.getMode().name());

            ItemStack held = stand.getHeldItem();
            String itemReg = !held.isEmpty() ? BuiltInRegistries.ITEM.getKey(held.getItem()).toString() : "minecraft:iron_sword";
            root.addProperty("baseItem", itemReg);
            root.addProperty("totalVoxels", stand.getTotalVoxelCount());
            root.addProperty("qualityMultiplier", stand.calculateQualityMultiplier());

            // Сохранение позы
            ItemPoseData pose = stand.getCurrentPose();
            JsonObject poseObj = new JsonObject();
            poseObj.addProperty("itemRotY", pose.itemRotY);
            poseObj.addProperty("armPitch", pose.armPitch);
            poseObj.addProperty("gripOffsetY", pose.gripOffsetY);
            poseObj.addProperty("twoHanded", pose.twoHanded);
            root.add("pose", poseObj);

            // Сохранение сеток вокселей (RLE + Base64)
            JsonObject gridsObj = new JsonObject();
            for (Map.Entry<String, VoxelGrid> entry : stand.getAllGrids().entrySet()) {
                if (entry.getValue().getNonEmptyCount() > 0) {
                    byte[] compressed = VoxelCompressor.compress(entry.getValue());
                    String b64 = Base64.getEncoder().encodeToString(compressed);
                    gridsObj.addProperty(entry.getKey(), b64);
                }
            }
            root.add("grids", gridsObj);

            File file = BLUEPRINTS_DIR.resolve(sanitizeFilename(name) + ".json").toFile();
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(root, writer);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Загрузка данных чертежа из файла.
     */
    public static BlueprintData loadBlueprint(String name) {
        init();
        File file = BLUEPRINTS_DIR.resolve(sanitizeFilename(name) + ".json").toFile();
        if (!file.exists()) {
            return null;
        }

        try (FileReader reader = new FileReader(file)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            BlueprintData data = new BlueprintData();
            data.name = root.has("name") ? root.get("name").getAsString() : name;
            data.author = root.has("author") ? root.get("author").getAsString() : "Unknown";

            if (root.has("mode")) {
                try {
                    data.mode = SculptStandBlockEntity.StandMode.valueOf(root.get("mode").getAsString());
                } catch (Exception ignored) {}
            }

            if (root.has("baseItem")) {
                data.baseItem = root.get("baseItem").getAsString();
            }

            if (root.has("totalVoxels")) {
                data.totalVoxels = root.get("totalVoxels").getAsInt();
            }

            if (root.has("qualityMultiplier")) {
                data.qualityModifier = root.get("qualityMultiplier").getAsFloat();
            }

            if (root.has("pose")) {
                JsonObject p = root.getAsJsonObject("pose");
                data.pose = new ItemPoseData();
                if (p.has("itemRotY")) data.pose.itemRotY = p.get("itemRotY").getAsFloat();
                if (p.has("armPitch")) data.pose.armPitch = p.get("armPitch").getAsFloat();
                if (p.has("gripOffsetY")) data.pose.gripOffsetY = p.get("gripOffsetY").getAsFloat();
                if (p.has("twoHanded")) data.pose.twoHanded = p.get("twoHanded").getAsBoolean();
            }

            if (root.has("grids")) {
                JsonObject gObj = root.getAsJsonObject("grids");
                for (String key : gObj.keySet()) {
                    String b64 = gObj.get(key).getAsString();
                    byte[] compressed = Base64.getDecoder().decode(b64);
                    data.grids.put(key, VoxelCompressor.decompress(compressed));
                }
            }

            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Проекция чертежа на стапель в виде голограммы-призрака (прозрачность 0.7).
     * Доступно в обычном выживании как руководство по ковке.
     */
    public static boolean applyGhostHologram(SculptStandBlockEntity stand, String name) {
        BlueprintData bp = loadBlueprint(name);
        if (bp == null) return false;

        stand.setGhostBlueprint(bp.name, bp.grids);
        return true;
    }

    /**
     * Мгновенное построение модели на стапеле (для креатива и тестирования).
     */
    public static boolean applyCreativeBuild(SculptStandBlockEntity stand, String name) {
        BlueprintData bp = loadBlueprint(name);
        if (bp == null) return false;

        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(bp.baseItem));
        if (item == Items.AIR) item = Items.IRON_SWORD;

        ItemStack stack = new ItemStack(item);
        stand.setItem(stack);

        // Замена сеток на сохраненные из чертежа
        for (Map.Entry<String, VoxelGrid> e : bp.grids.entrySet()) {
            stand.getGrid(e.getKey()).copyFrom(e.getValue());
        }

        if (bp.pose != null) {
            stand.getCurrentPose().copyFrom(bp.pose);
        }

        stand.setChanged();
        return true;
    }

    /**
     * Мгновенное создание готового запечатанного предмета прямо из чертежа (для креатива).
     */
    public static ItemStack createItemFromBlueprint(String name) {
        return createItemFromBlueprint(name, null);
    }

    public static ItemStack createItemFromBlueprint(String name, @org.jetbrains.annotations.Nullable net.minecraft.core.HolderLookup.Provider registries) {
        BlueprintData bp = loadBlueprint(name);
        if (bp == null) return ItemStack.EMPTY;

        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(bp.baseItem));
        if (item == Items.AIR) item = Items.IRON_SWORD;

        ItemStack stack = new ItemStack(item);
        CompoundTag rootTag = new CompoundTag();

        boolean isArmor = item instanceof ArmorItem;
        float quality = bp.qualityModifier > 0 ? bp.qualityModifier : 1.0f;
        ItemStatData stats = StatCalculator.calculate(bp.grids, isArmor, stack, quality);

        // 1. Воксели
        if (bp.mode == SculptStandBlockEntity.StandMode.WEAPON) {
            VoxelGrid g = bp.grids.get("weapon");
            if (g != null && g.getNonEmptyCount() > 0) {
                rootTag.putByteArray(VoxelConstants.NBT_VOXEL_DATA, VoxelCompressor.compress(g));
            }
        } else {
            CompoundTag modelTag = new CompoundTag();
            for (Map.Entry<String, VoxelGrid> entry : bp.grids.entrySet()) {
                if (entry.getValue().getNonEmptyCount() > 0) {
                    modelTag.putByteArray(entry.getKey(), VoxelCompressor.compress(entry.getValue()));
                }
            }
            rootTag.put(VoxelConstants.NBT_VOXEL_MODEL, modelTag);
        }

        // 2. RPG характеристики
        rootTag.put("CustomSmithStats", stats.toNBT());
        rootTag.putBoolean("CustomSmithForged", true);
        rootTag.putBoolean("CustomSmithBaked", true);
        rootTag.putDouble("totalWeight", stats.weight);
        rootTag.putDouble("attackSpeed", stats.attackSpeed);
        rootTag.putString("weightClass", stats.weightClass.name());
        rootTag.putFloat("QualityModifier", quality);
        rootTag.putString("QualityName", quality >= 1.25f ? "Чистое" : (quality >= 1.10f ? "Крепкое" : "Обычное"));

        // 3. Зачарования
        if (registries != null) {
            var lookup = registries.lookup(net.minecraft.core.registries.Registries.ENCHANTMENT);
            if (lookup.isPresent()) {
                for (Map.Entry<ResourceLocation, Integer> entry : stats.enchantments.entrySet()) {
                    var enchHolder = lookup.get().get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, entry.getKey()));
                    enchHolder.ifPresent(holder -> stack.enchant(holder, entry.getValue()));
                }
            }
        }

        // 4. Поза
        if (bp.pose != null) {
            rootTag.put(VoxelConstants.NBT_POSE_DATA, bp.pose.toNbt());
        }

        com.voxelstand.api.VoxelDataHelper.setCustomData(stack, rootTag);
        return stack;
    }

    /**
     * Список доступных чертежей из папки.
     */
    public static List<String> listBlueprints() {
        init();
        List<String> list = new ArrayList<>();
        File dir = BLUEPRINTS_DIR.toFile();
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, f) -> f.endsWith(".json"));
            if (files != null) {
                for (File f : files) {
                    String n = f.getName();
                    list.add(n.substring(0, n.length() - 5));
                }
            }
        }
        return list;
    }

    private static String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
