package com.voxelstand.block.entity;

import com.voxelstand.api.ItemPoseData;
import com.voxelstand.api.VoxelConstants;
import com.voxelstand.stats.ItemStatData;
import com.voxelstand.stats.StatCalculator;
import com.voxelstand.voxel.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

public class SculptStandBlockEntity extends BlockEntity {
    public enum StandMode {
        EMPTY,
        WEAPON,
        HELMET,
        CHESTPLATE,
        LEGGINGS,
        BOOTS
    }

    private StandMode mode = StandMode.EMPTY;
    private ItemStack heldItem = ItemStack.EMPTY;
    private final Map<String, VoxelGrid> grids = new HashMap<>();
    private boolean symmetryMode = true;
    private int guideMode = 0; // 0 = Солома, 1 = Каркас и веревки, 2 = Скрыт
    private long lastFinalizeAttempt = 0;
    private boolean isPoseCustomizationOnly = false;
    private ItemPoseData currentPose = new ItemPoseData();

    // Трекинг качества вложенных пластин (Normal, Sturdy, Pure)
    private int platesNormal = 0;
    private int platesSturdy = 0;
    private int platesPure = 0;

    // Голограмма-призрак чертежа (Ghost Blueprint с прозрачностью 0.7)
    private boolean hasGhostBlueprint = false;
    private String ghostBlueprintName = "";
    private final Map<String, VoxelGrid> ghostGrids = new HashMap<>();

    public void recordPlatePlacement(com.voxelstand.item.PlateQuality quality, int count) {
        if (quality == com.voxelstand.item.PlateQuality.PURE) {
            platesPure += count;
        } else if (quality == com.voxelstand.item.PlateQuality.STURDY) {
            platesSturdy += count;
        } else {
            platesNormal += count;
        }
        setChanged();
    }

    public float calculateQualityMultiplier() {
        int total = platesNormal + platesSturdy + platesPure;
        if (total <= 0) return 1.0f;
        float weighted = (platesNormal * 1.00f) + (platesSturdy * 1.15f) + (platesPure * 1.30f);
        return weighted / (float) total;
    }

    public String getQualityName() {
        float mult = calculateQualityMultiplier();
        if (mult >= 1.25f) return "Чистое";
        if (mult >= 1.10f) return "Крепкое";
        return "Обычное";
    }

    public int getPlatesNormal() { return platesNormal; }
    public int getPlatesSturdy() { return platesSturdy; }
    public int getPlatesPure() { return platesPure; }

    public boolean hasGhostBlueprint() { return hasGhostBlueprint; }
    public String getGhostBlueprintName() { return ghostBlueprintName; }
    public Map<String, VoxelGrid> getGhostGrids() { return ghostGrids; }

    public void setGhostBlueprint(String name, Map<String, VoxelGrid> blueprintGrids) {
        this.hasGhostBlueprint = true;
        this.ghostBlueprintName = name != null ? name : "Blueprint";
        this.ghostGrids.clear();
        if (blueprintGrids != null) {
            for (Map.Entry<String, VoxelGrid> e : blueprintGrids.entrySet()) {
                this.ghostGrids.put(e.getKey(), e.getValue().copy());
            }
        }
        setChanged();
    }

    public void clearGhostBlueprint() {
        this.hasGhostBlueprint = false;
        this.ghostBlueprintName = "";
        this.ghostGrids.clear();
        setChanged();
    }

    public boolean isPoseCustomizationOnly() {
        return isPoseCustomizationOnly;
    }

    public ItemPoseData getCurrentPose() {
        return currentPose;
    }

    public void cyclePoseRotation() {
        currentPose.itemRotY = (currentPose.itemRotY + 90.0f) % 360.0f;
        setChanged();
    }

    public void toggleTwoHanded() {
        currentPose.twoHanded = !currentPose.twoHanded;
        setChanged();
    }

    public void adjustArmPitch(float delta) {
        currentPose.armPitch = Math.max(-90.0f, Math.min(90.0f, currentPose.armPitch + delta));
        setChanged();
    }

    public void adjustGripOffset(float delta) {
        currentPose.gripOffsetY += delta;
        setChanged();
    }

    public SculptStandBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SCULPT_STAND.get(), pos, state);
    }

    public StandMode getMode() {
        return mode;
    }

    public ItemStack getHeldItem() {
        return heldItem;
    }

    public boolean isSymmetryMode() {
        return symmetryMode;
    }

    public void setSymmetryMode(boolean mode) {
        this.symmetryMode = mode;
        setChanged();
    }

    public int getGuideMode() {
        return guideMode;
    }

    public void cycleGuideMode() {
        this.guideMode = (this.guideMode + 1) % 3;
        setChanged();
    }

    public VoxelGrid getGrid(String part) {
        return grids.computeIfAbsent(part, k -> new VoxelGrid());
    }

    public Map<String, VoxelGrid> getAllGrids() {
        return grids;
    }

    public float getPartOffset(String part) {
        float baseGap = 0.35f;
        if ("left_arm".equals(part) || "right_arm".equals(part) ||
            "left_leg".equals(part) || "right_leg".equals(part) ||
            "left_boot".equals(part) || "right_boot".equals(part)) {
            VoxelGrid grid = grids.get(part);
            int extraWidth = 0;
            if (grid != null && grid.getNonEmptyCount() > 0) {
                extraWidth = Math.min(8, grid.getNonEmptyCount() / 256);
            }
            return baseGap + (extraWidth * 0.02f);
        }
        return 0.0f;
    }

    public int getTotalVoxelCount() {
        int total = 0;
        for (VoxelGrid grid : grids.values()) {
            total += grid.getNonEmptyCount();
        }
        return total;
    }

    public long getLastFinalizeAttempt() {
        return lastFinalizeAttempt;
    }

    public void setLastFinalizeAttempt(long time) {
        this.lastFinalizeAttempt = time;
    }

    /**
     * Установка предмета на стойку.
     * В Full-версии:
     * 1. Если предмет уже скован (CustomSmithForged) — разрешает вход в режим ТОЛЬКО настройки позы и хвата!
     * 2. Ванильные предметы из данжей стандартизируются и очищаются от старых чар.
     */
    public boolean setItem(ItemStack stack) {
        if (stack.isEmpty()) return false;

        boolean isAlreadyForged = com.voxelstand.api.VoxelDataHelper.hasCustomKey(stack, "CustomSmithForged") 
                && com.voxelstand.api.VoxelDataHelper.getCustomData(stack).getBoolean("CustomSmithForged");
        StandMode detectedMode = detectMode(stack);
        if (detectedMode == StandMode.EMPTY) return false;

        this.heldItem = stack.copy();
        this.mode = detectedMode;
        this.grids.clear();
        this.isPoseCustomizationOnly = isAlreadyForged;

        if (isAlreadyForged) {
            // Загрузка сохраненных вокселей без изменения геометрии и статов
            loadItemVoxels(this.heldItem);
            CompoundTag heldTag = com.voxelstand.api.VoxelDataHelper.getCustomData(this.heldItem);
            if (heldTag.contains(VoxelConstants.NBT_POSE_DATA)) {
                this.currentPose = ItemPoseData.fromNbt(heldTag.getCompound(VoxelConstants.NBT_POSE_DATA));
            } else {
                this.currentPose = new ItemPoseData();
            }
        } else {
            this.currentPose = new ItemPoseData();
            // Автовокселизация базового каркаса
            applyAutoVoxelization(this.heldItem);
        }

        setChanged();
        return true;
    }

    private void loadItemVoxels(ItemStack stack) {
        if (!com.voxelstand.api.VoxelDataHelper.hasCustomData(stack)) return;
        CompoundTag tag = com.voxelstand.api.VoxelDataHelper.getCustomData(stack);
        if (mode == StandMode.WEAPON && tag.contains(VoxelConstants.NBT_VOXEL_DATA)) {
            byte[] data = tag.getByteArray(VoxelConstants.NBT_VOXEL_DATA);
            grids.put("weapon", VoxelCompressor.decompress(data));
        } else if (tag.contains(VoxelConstants.NBT_VOXEL_MODEL)) {
            CompoundTag model = tag.getCompound(VoxelConstants.NBT_VOXEL_MODEL);
            for (String key : model.getAllKeys()) {
                grids.put(key, VoxelCompressor.decompress(model.getByteArray(key)));
            }
        }
    }

    private void applyAutoVoxelization(ItemStack stack) {
        switch (mode) {
            case WEAPON -> {
                VoxelGrid g = getGrid("weapon");
                if (stack.getItem() instanceof ShieldItem) {
                    AutoVoxelizer.voxelizeShield(stack, g);
                } else if (stack.getItem() instanceof BowItem) {
                    AutoVoxelizer.voxelizeBow(stack, g);
                } else if (stack.getItem() instanceof CrossbowItem) {
                    AutoVoxelizer.voxelizeCrossbow(stack, g);
                } else {
                    AutoVoxelizer.voxelizeWeapon(stack, g);
                }
            }
            case HELMET -> {
                VoxelGrid head = getGrid("head");
                AutoVoxelizer.voxelizeHelmet(stack, head);
            }
            case CHESTPLATE -> {
                VoxelGrid body = getGrid("body");
                VoxelGrid leftArm = getGrid("left_arm");
                VoxelGrid rightArm = getGrid("right_arm");
                AutoVoxelizer.voxelizeChestplate(stack, body, leftArm, rightArm);
            }
            case LEGGINGS -> {
                VoxelGrid pelvis = getGrid("pelvis");
                VoxelGrid leftLeg = getGrid("left_leg");
                VoxelGrid rightLeg = getGrid("right_leg");
                AutoVoxelizer.voxelizeLeggings(stack, pelvis, leftLeg, rightLeg);
            }
            case BOOTS -> {
                VoxelGrid leftBoot = getGrid("left_boot");
                VoxelGrid rightBoot = getGrid("right_boot");
                AutoVoxelizer.voxelizeBoots(stack, leftBoot, rightBoot);
            }
            default -> {}
        }
    }

    private StandMode detectMode(ItemStack stack) {
        if (stack.getItem() instanceof TieredItem ||
            stack.getItem() instanceof ShieldItem ||
            stack.getItem() instanceof BowItem ||
            stack.getItem() instanceof CrossbowItem) {
            return StandMode.WEAPON;
        }
        if (stack.getItem() instanceof ArmorItem armor) {
            EquipmentSlot slot = armor.getEquipmentSlot();
            return switch (slot) {
                case HEAD -> StandMode.HELMET;
                case CHEST -> StandMode.CHESTPLATE;
                case LEGS -> StandMode.LEGGINGS;
                case FEET -> StandMode.BOOTS;
                default -> StandMode.EMPTY;
            };
        }
        return StandMode.EMPTY;
    }

    /**
     * Окончательное запечатывание предмета (Finalize & Extract).
     * Вычисляет урон, вес, прочность, чары от материалов и ставит метку CustomSmithForged.
     */
    public ItemStack finalizeAndExtractItem() {
        if (heldItem.isEmpty()) return ItemStack.EMPTY;

        ItemStack result = heldItem.copy();
        CompoundTag rootTag = com.voxelstand.api.VoxelDataHelper.getCustomData(result);

        // Если это повторная установка для смены позы: обновляем ТОЛЬКО позу, не трогая статы!
        if (isPoseCustomizationOnly) {
            rootTag.put(VoxelConstants.NBT_POSE_DATA, currentPose.toNbt());
        com.voxelstand.api.VoxelDataHelper.setCustomData(result, rootTag);
            com.voxelstand.api.VoxelDataHelper.setCustomData(result, rootTag);
            this.heldItem = ItemStack.EMPTY;
            this.mode = StandMode.EMPTY;
            this.grids.clear();
            this.isPoseCustomizationOnly = false;
            setChanged();
            return result;
        }

        if (getTotalVoxelCount() > VoxelConstants.DEFAULT_VOXEL_LIMIT) {
            return ItemStack.EMPTY;
        }

        float qualityMultiplier = calculateQualityMultiplier();
        String qualityName = getQualityName();
        boolean isArmor = heldItem.getItem() instanceof ArmorItem;
        ItemStatData stats = StatCalculator.calculate(grids, isArmor, heldItem, qualityMultiplier);

        // 1. Сохранение вокселей для 3D рендера
        if (mode == StandMode.WEAPON) {
            VoxelGrid g = grids.get("weapon");
            if (g != null && g.getNonEmptyCount() > 0) {
                rootTag.putByteArray(VoxelConstants.NBT_VOXEL_DATA, VoxelCompressor.compress(g));
            }
        } else {
            CompoundTag modelTag = new CompoundTag();
            for (Map.Entry<String, VoxelGrid> entry : grids.entrySet()) {
                if (entry.getValue().getNonEmptyCount() > 0) {
                    modelTag.putByteArray(entry.getKey(), VoxelCompressor.compress(entry.getValue()));
                }
            }
            rootTag.put(VoxelConstants.NBT_VOXEL_MODEL, modelTag);
        }

        // 2. Запись RPG-характеристик и качества
        rootTag.put("CustomSmithStats", stats.toNBT());
        rootTag.putBoolean("CustomSmithForged", true);
        rootTag.putBoolean("CustomSmithBaked", true);
        rootTag.putDouble("totalWeight", stats.weight);
        rootTag.putDouble("attackSpeed", stats.attackSpeed);
        rootTag.putString("weightClass", stats.weightClass.name());
        rootTag.putFloat("QualityModifier", qualityMultiplier);
        rootTag.putString("QualityName", qualityName);
        rootTag.putInt("PlatesNormal", platesNormal);
        rootTag.putInt("PlatesSturdy", platesSturdy);
        rootTag.putInt("PlatesPure", platesPure);

        // 3. Запись вычисленных ванильных зачарований
        if (this.level != null) {
            var lookup = this.level.registryAccess().lookup(Registries.ENCHANTMENT);
            if (lookup.isPresent()) {
                for (Map.Entry<ResourceLocation, Integer> entry : stats.enchantments.entrySet()) {
                    var enchHolder = lookup.get().get(ResourceKey.create(Registries.ENCHANTMENT, entry.getKey()));
                    enchHolder.ifPresent(holder -> result.enchant(holder, entry.getValue()));
                }
            }
        }

        // 4. Запись позы (умная рекомендация по умолчанию)
        if (currentPose == null || (currentPose.armPitch == 0 && currentPose.itemRotY == 0 && !currentPose.twoHanded)) {
            currentPose = ItemPoseData.createDefault(stats.totalWeight, heldItem.getItem() instanceof ShieldItem);
        }
        rootTag.put(VoxelConstants.NBT_POSE_DATA, currentPose.toNbt());
        com.voxelstand.api.VoxelDataHelper.setCustomData(result, rootTag);

        // Сброс стойки
        this.heldItem = ItemStack.EMPTY;
        this.mode = StandMode.EMPTY;
        this.grids.clear();
        this.lastFinalizeAttempt = 0;
        this.isPoseCustomizationOnly = false;
        this.platesNormal = 0;
        this.platesSturdy = 0;
        this.platesPure = 0;
        setChanged();

        return result;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("StandMode", mode.name());
        if (!heldItem.isEmpty()) {
            tag.put("HeldItem", heldItem.save(registries));
        }

        CompoundTag partsTag = new CompoundTag();
        for (Map.Entry<String, VoxelGrid> entry : grids.entrySet()) {
            if (entry.getValue().getNonEmptyCount() > 0) {
                partsTag.putByteArray(entry.getKey(), VoxelCompressor.compress(entry.getValue()));
            }
        }
        tag.put("GridsData", partsTag);
        tag.putBoolean("SymmetryMode", symmetryMode);
        tag.putInt("GuideMode", guideMode);
        tag.putBoolean("IsPoseOnly", isPoseCustomizationOnly);
        tag.put("CurrentPose", currentPose.toNbt());

        tag.putInt("PlatesNormal", platesNormal);
        tag.putInt("PlatesSturdy", platesSturdy);
        tag.putInt("PlatesPure", platesPure);

        tag.putBoolean("HasGhostBlueprint", hasGhostBlueprint);
        tag.putString("GhostBlueprintName", ghostBlueprintName != null ? ghostBlueprintName : "");
        if (hasGhostBlueprint && !ghostGrids.isEmpty()) {
            CompoundTag ghostTag = new CompoundTag();
            for (Map.Entry<String, VoxelGrid> entry : ghostGrids.entrySet()) {
                if (entry.getValue().getNonEmptyCount() > 0) {
                    ghostTag.putByteArray(entry.getKey(), VoxelCompressor.compress(entry.getValue()));
                }
            }
            tag.put("GhostGridsData", ghostTag);
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("StandMode")) {
            try {
                this.mode = StandMode.valueOf(tag.getString("StandMode"));
            } catch (Exception e) {
                this.mode = StandMode.EMPTY;
            }
        } else {
            this.mode = StandMode.EMPTY;
        }

        if (tag.contains("HeldItem")) {
            this.heldItem = ItemStack.parse(registries, tag.getCompound("HeldItem")).orElse(ItemStack.EMPTY);
        } else {
            this.heldItem = ItemStack.EMPTY;
        }

        this.grids.clear();
        if (tag.contains("GridsData")) {
            CompoundTag partsTag = tag.getCompound("GridsData");
            for (String key : partsTag.getAllKeys()) {
                grids.put(key, VoxelCompressor.decompress(partsTag.getByteArray(key)));
            }
        }

        this.symmetryMode = tag.getBoolean("SymmetryMode");
        this.guideMode = tag.getInt("GuideMode");
        this.isPoseCustomizationOnly = tag.getBoolean("IsPoseOnly");
        this.currentPose = tag.contains("CurrentPose") ? ItemPoseData.fromNbt(tag.getCompound("CurrentPose")) : new ItemPoseData();

        this.platesNormal = tag.getInt("PlatesNormal");
        this.platesSturdy = tag.getInt("PlatesSturdy");
        this.platesPure = tag.getInt("PlatesPure");

        this.hasGhostBlueprint = tag.getBoolean("HasGhostBlueprint");
        this.ghostBlueprintName = tag.getString("GhostBlueprintName");
        this.ghostGrids.clear();
        if (tag.contains("GhostGridsData")) {
            CompoundTag ghostTag = tag.getCompound("GhostGridsData");
            for (String key : ghostTag.getAllKeys()) {
                ghostGrids.put(key, VoxelCompressor.decompress(ghostTag.getByteArray(key)));
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
