package com.voxelstand.stats;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Структура вычисленных динамических характеристик предмета на основе его вокселей.
 */
public class ItemStatData {
    public enum WeightClass {
        LIGHT("Легкий", "§a[ ЛЕГКИЙ ]", 0x55FF55),
        MEDIUM("Средний", "§e[ СРЕДНИЙ ]", 0xFFFF55),
        HEAVY("Тяжелый", "§c[ ТЯЖЕЛЫЙ ]", 0xFF5555);

        private final String displayName;
        private final String formattedBadge;
        private final int colorRgb;

        WeightClass(String displayName, String formattedBadge, int colorRgb) {
            this.displayName = displayName;
            this.formattedBadge = formattedBadge;
            this.colorRgb = colorRgb;
        }

        public String getDisplayName() { return displayName; }
        public String getFormattedBadge() { return formattedBadge; }
        public int getColorRgb() { return colorRgb; }
    }

    public float damage;
    public float attackSpeed;
    public float armor;
    public float armorToughness;
    public float knockbackResistance;
    public float weight;
    public float totalWeight;
    public float movementSpeedPenalty;
    public float miningSpeedModifier;
    public int miningTier;
    public int maxDurability;
    public byte dominantMaterialId;
    public boolean isBroken;

    public WeightClass weightClass = WeightClass.MEDIUM;
    public float drawSpeedTicks = 20.0f;
    public float arrowVelocityMult = 1.0f;
    public float armorPenetration = 0.0f;
    public float aimSway = 0.0f;
    public int totalVoxels = 0;
    public float inlayRatio = 0.0f;

    // Модификаторы качества пластин
    public float qualityMultiplier = 1.0f;
    public String qualityName = "Обычное";

    public final Map<ResourceLocation, Integer> enchantments = new HashMap<>();

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Damage", damage);
        tag.putFloat("AttackSpeed", attackSpeed);
        tag.putFloat("Armor", armor);
        tag.putFloat("ArmorToughness", armorToughness);
        tag.putFloat("KnockbackResistance", knockbackResistance);
        tag.putFloat("Weight", weight);
        tag.putFloat("SpeedPenalty", movementSpeedPenalty);
        tag.putFloat("MiningSpeedMod", miningSpeedModifier);
        tag.putInt("MiningTier", miningTier);
        tag.putInt("MaxDurability", maxDurability);
        tag.putByte("DominantMat", dominantMaterialId);
        tag.putBoolean("Broken", isBroken);
        tag.putString("WeightClass", weightClass.name());
        tag.putFloat("DrawSpeedTicks", drawSpeedTicks);
        tag.putFloat("ArrowVelocityMult", arrowVelocityMult);
        tag.putFloat("ArmorPenetration", armorPenetration);
        tag.putFloat("AimSway", aimSway);
        tag.putInt("TotalVoxels", totalVoxels);
        tag.putFloat("InlayRatio", inlayRatio);
        tag.putFloat("QualityModifier", qualityMultiplier);
        tag.putString("QualityName", qualityName);

        ListTag enchList = new ListTag();
        for (Map.Entry<ResourceLocation, Integer> entry : enchantments.entrySet()) {
            CompoundTag eTag = new CompoundTag();
            eTag.putString("id", entry.getKey().toString());
            eTag.putShort("lvl", entry.getValue().shortValue());
            enchList.add(eTag);
        }
        tag.put("CustomEnchantments", enchList);

        return tag;
    }

    public static ItemStatData fromNBT(CompoundTag tag) {
        ItemStatData data = new ItemStatData();
        if (tag == null) return data;

        data.damage = tag.getFloat("Damage");
        data.attackSpeed = tag.getFloat("AttackSpeed");
        data.armor = tag.getFloat("Armor");
        data.armorToughness = tag.getFloat("ArmorToughness");
        data.knockbackResistance = tag.getFloat("KnockbackResistance");
        data.weight = tag.getFloat("Weight");
        data.totalWeight = data.weight;
        data.movementSpeedPenalty = tag.getFloat("SpeedPenalty");
        data.miningSpeedModifier = tag.getFloat("MiningSpeedMod");
        data.miningTier = tag.getInt("MiningTier");
        data.maxDurability = tag.getInt("MaxDurability");
        data.dominantMaterialId = tag.getByte("DominantMat");
        data.isBroken = tag.getBoolean("Broken");
        data.qualityMultiplier = tag.contains("QualityModifier") ? tag.getFloat("QualityModifier") : 1.0f;
        data.qualityName = tag.contains("QualityName") ? tag.getString("QualityName") : "Обычное";

        if (tag.contains("WeightClass")) {
            try {
                data.weightClass = WeightClass.valueOf(tag.getString("WeightClass"));
            } catch (Exception ignored) {
                data.weightClass = WeightClass.MEDIUM;
            }
        }
        data.drawSpeedTicks = tag.contains("DrawSpeedTicks") ? tag.getFloat("DrawSpeedTicks") : 20.0f;
        data.arrowVelocityMult = tag.contains("ArrowVelocityMult") ? tag.getFloat("ArrowVelocityMult") : 1.0f;
        data.armorPenetration = tag.getFloat("ArmorPenetration");
        data.aimSway = tag.getFloat("AimSway");
        data.totalVoxels = tag.getInt("TotalVoxels");
        data.inlayRatio = tag.getFloat("InlayRatio");

        if (tag.contains("CustomEnchantments", 9)) {
            ListTag enchList = tag.getList("CustomEnchantments", 10);
            for (int i = 0; i < enchList.size(); i++) {
                CompoundTag eTag = enchList.getCompound(i);
                try {
                    data.enchantments.put(ResourceLocation.parse(eTag.getString("id")), (int) eTag.getShort("lvl"));
                } catch (Exception ignored) {}
            }
        }

        return data;
    }
}
