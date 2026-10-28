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
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

public class SculptStandBlockEntity extends BlockEntity {
    public enum StandMode implements StringRepresentable {
        EMPTY("empty"),
        WEAPON("weapon"),
        HELMET("helmet"),
        CHESTPLATE("chestplate"),
        LEGGINGS("leggings"),
        BOOTS("boots");

        private final String name;
        StandMode(String name) { this.name = name; }
        @Override
        public String getSerializedName() { return this.name; }
    }

    private StandMode mode = StandMode.EMPTY;
    private ItemStack heldItem = ItemStack.EMPTY;
    private final Map<String, VoxelGrid> grids = new HashMap<>();
    private boolean symmetryMode = true;
    private int guideMode = 0; // 0 = РЎРѕР»РѕРјР°, 1 = РљР°СЂРєР°СЃ Рё РІРµСЂРµРІРєРё, 2 = РЎРєСЂС‹С‚
    private long lastFinalizeAttempt = 0;
    private boolean isPoseCustomizationOnly = false;
    private ItemPoseData currentPose = new ItemPoseData();

    // РўСЂРµРєРёРЅРі РєР°С‡РµСЃС‚РІР° РІР»РѕР¶РµРЅРЅС‹С… РїР»Р°СЃС‚РёРЅ (Normal, Sturdy, Pure)
    private int platesNormal = 0;
    private int platesSturdy = 0;
    private int platesPure = 0;

    // Р“РѕР»РѕРіСЂР°РјРјР°-РїСЂРёР·СЂР°Рє С‡РµСЂС‚РµР¶Р° (Ghost Blueprint СЃ РїСЂРѕР·СЂР°С‡РЅРѕСЃС‚СЊСЋ 0.7)
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
     * РЈСЃС‚Р°РЅРѕРІРєР° РїСЂРµРґРјРµС‚Р° РЅР° СЃС‚РѕР№РєСѓ.
     * Р’ Full-РІРµСЂСЃРёРё:
     * 1. Р•СЃР»Рё РїСЂРµРґРјРµС‚ СѓР¶Рµ СЃРєРѕРІР°РЅ (CustomSmithForged) вЂ” СЂР°Р·СЂРµС€Р°РµС‚ РІС…РѕРґ РІ СЂРµР¶РёРј РўРћР›Р¬РљРћ РЅР°СЃС‚СЂРѕР№РєРё РїРѕР·С‹ Рё С…РІР°С‚Р°!
     * 2. Р’Р°РЅРёР»СЊРЅС‹Рµ РїСЂРµРґРјРµС‚С‹ РёР· РґР°РЅР¶РµР№ СЃС‚Р°РЅРґР°СЂС‚РёР·РёСЂСѓСЋС‚СЃСЏ Рё РѕС‡РёС‰Р°СЋС‚СЃСЏ РѕС‚ СЃС‚Р°СЂС‹С… С‡Р°СЂ.
     */
    public boolean canAcceptItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof com.voxelstand.item.ChiselItem ||
            stack.getItem() instanceof com.voxelstand.item.PlateItem) {
            return false;
        }
        return detectMode(stack) != StandMode.EMPTY;
    }

    public boolean setItem(ItemStack stack) {
        if (!canAcceptItem(stack)) return false;

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
        if (this.level != null && !this.level.isClientSide) {
            BlockState bState = this.level.getBlockState(this.worldPosition);
            if (bState.hasProperty(com.voxelstand.block.SculptStandBlock.MODE)) {
                this.level.setBlock(this.worldPosition, bState.setValue(com.voxelstand.block.SculptStandBlock.MODE, this.mode), 3);
            }
        }
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
     * РћРєРѕРЅС‡Р°С‚РµР»СЊРЅРѕРµ Р·Р°РїРµС‡Р°С‚С‹РІР°РЅРёРµ РїСЂРµРґРјРµС‚Р° (Finalize & Extract).
     * Р’С‹С‡РёСЃР»СЏРµС‚ СѓСЂРѕРЅ, РІРµСЃ, РїСЂРѕС‡РЅРѕСЃС‚СЊ, С‡Р°СЂС‹ РѕС‚ РјР°С‚РµСЂРёР°Р»РѕРІ Рё СЃС‚Р°РІРёС‚ РјРµС‚РєСѓ CustomSmithForged.
     */
    public ItemStack finalizeAndExtractItem() {
        if (heldItem.isEmpty()) return ItemStack.EMPTY;

        ItemStack result = heldItem.copy();
        CompoundTag rootTag = com.voxelstand.api.VoxelDataHelper.getCustomData(result);

        // Р•СЃР»Рё СЌС‚Рѕ РїРѕРІС‚РѕСЂРЅР°СЏ СѓСЃС‚Р°РЅРѕРІРєР° РґР»СЏ СЃРјРµРЅС‹ РїРѕР·С‹: РѕР±РЅРѕРІР»СЏРµРј РўРћР›Р¬РљРћ РїРѕР·Сѓ, РЅРµ С‚СЂРѕРіР°СЏ СЃС‚Р°С‚С‹!
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

        // 1. РЎРѕС…СЂР°РЅРµРЅРёРµ РІРѕРєСЃРµР»РµР№ РґР»СЏ 3D СЂРµРЅРґРµСЂР°
        if (mode == StandMode.WEAPON) {
            VoxelGrid g = grids.get("weapon");
            if (g != null && g.getNonEmptyCount() > 0) {
                rootTag.putByteArray(VoxelConstants.NBT_VOXEL_DATA, VoxelCompressor.compress(g));
            }
        } else {
            CompoundTag modelTag = new CompoundTag();
            byte[] mainPartCompressed = null;
            for (Map.Entry<String, VoxelGrid> entry : grids.entrySet()) {
                if (entry.getValue().getNonEmptyCount() > 0) {
                    byte[] comp = VoxelCompressor.compress(entry.getValue());
                    modelTag.putByteArray(entry.getKey(), comp);
                    if (mainPartCompressed == null || entry.getKey().equals("body") || entry.getKey().equals("head") || entry.getKey().equals("pelvis")) {
                        mainPartCompressed = comp;
                    }
                }
            }
            rootTag.put(VoxelConstants.NBT_VOXEL_MODEL, modelTag);
            if (mainPartCompressed != null) {
                rootTag.putByteArray(VoxelConstants.NBT_VOXEL_DATA, mainPartCompressed);
            }
        }

        // 2. Р—Р°РїРёСЃСЊ RPG-С…Р°СЂР°РєС‚РµСЂРёСЃС‚РёРє Рё РєР°С‡РµСЃС‚РІР°
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

        // 3. Р—Р°РїРёСЃСЊ РІС‹С‡РёСЃР»РµРЅРЅС‹С… РІР°РЅРёР»СЊРЅС‹С… Р·Р°С‡Р°СЂРѕРІР°РЅРёР№
        if (this.level != null) {
            var lookup = this.level.registryAccess().lookup(Registries.ENCHANTMENT);
            if (lookup.isPresent()) {
                for (Map.Entry<ResourceLocation, Integer> entry : stats.enchantments.entrySet()) {
                    var enchHolder = lookup.get().get(ResourceKey.create(Registries.ENCHANTMENT, entry.getKey()));
                    enchHolder.ifPresent(holder -> result.enchant(holder, entry.getValue()));
                }
            }
        }

        // 4. Р—Р°РїРёСЃСЊ РїРѕР·С‹ (СѓРјРЅР°СЏ СЂРµРєРѕРјРµРЅРґР°С†РёСЏ РїРѕ СѓРјРѕР»С‡Р°РЅРёСЋ)
        if (currentPose == null || (currentPose.armPitch == 0 && currentPose.itemRotY == 0 && !currentPose.twoHanded)) {
            currentPose = ItemPoseData.createDefault(stats.totalWeight, heldItem.getItem() instanceof ShieldItem);
        }
        rootTag.put(VoxelConstants.NBT_POSE_DATA, currentPose.toNbt());
        com.voxelstand.api.VoxelDataHelper.setCustomData(result, rootTag);

        // РЎР±СЂРѕСЃ СЃС‚РѕР№РєРё
        this.heldItem = ItemStack.EMPTY;
        this.mode = StandMode.EMPTY;
        this.grids.clear();
        this.lastFinalizeAttempt = 0;
        this.isPoseCustomizationOnly = false;
        this.platesNormal = 0;
        this.platesSturdy = 0;
        this.platesPure = 0;
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            BlockState bState = this.level.getBlockState(this.worldPosition);
            if (bState.hasProperty(com.voxelstand.block.SculptStandBlock.MODE)) {
                this.level.setBlock(this.worldPosition, bState.setValue(com.voxelstand.block.SculptStandBlock.MODE, StandMode.EMPTY), 3);
            }
        }

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

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            loadAdditional(tag, lookupProvider);
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }
}
