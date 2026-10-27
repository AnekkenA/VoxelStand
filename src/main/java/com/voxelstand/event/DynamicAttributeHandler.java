package com.voxelstand.event;

import com.voxelstand.VoxelStandMod;
import com.voxelstand.api.VoxelDataHelper;
import com.voxelstand.stats.ItemStatData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = VoxelStandMod.MOD_ID)
public class DynamicAttributeHandler {

    private static final ResourceLocation BASE_ATTACK_DAMAGE_ID = ResourceLocation.fromNamespaceAndPath(VoxelStandMod.MOD_ID, "base_attack_damage");
    private static final ResourceLocation BASE_ATTACK_SPEED_ID = ResourceLocation.fromNamespaceAndPath(VoxelStandMod.MOD_ID, "base_attack_speed");

    private static final ResourceLocation[] ARMOR_MODIFIER_IDS = new ResourceLocation[] {
            ResourceLocation.fromNamespaceAndPath(VoxelStandMod.MOD_ID, "armor_feet"),
            ResourceLocation.fromNamespaceAndPath(VoxelStandMod.MOD_ID, "armor_legs"),
            ResourceLocation.fromNamespaceAndPath(VoxelStandMod.MOD_ID, "armor_chest"),
            ResourceLocation.fromNamespaceAndPath(VoxelStandMod.MOD_ID, "armor_head")
    };

    private static final ResourceLocation[] TOUGHNESS_MODIFIER_IDS = new ResourceLocation[] {
            ResourceLocation.fromNamespaceAndPath(VoxelStandMod.MOD_ID, "toughness_feet"),
            ResourceLocation.fromNamespaceAndPath(VoxelStandMod.MOD_ID, "toughness_legs"),
            ResourceLocation.fromNamespaceAndPath(VoxelStandMod.MOD_ID, "toughness_chest"),
            ResourceLocation.fromNamespaceAndPath(VoxelStandMod.MOD_ID, "toughness_head")
    };

    private static final ResourceLocation[] KNOCKBACK_MODIFIER_IDS = new ResourceLocation[] {
            ResourceLocation.fromNamespaceAndPath(VoxelStandMod.MOD_ID, "knockback_feet"),
            ResourceLocation.fromNamespaceAndPath(VoxelStandMod.MOD_ID, "knockback_legs"),
            ResourceLocation.fromNamespaceAndPath(VoxelStandMod.MOD_ID, "knockback_chest"),
            ResourceLocation.fromNamespaceAndPath(VoxelStandMod.MOD_ID, "knockback_head")
    };

    private static final ResourceLocation[] SPEED_PENALTY_IDS = new ResourceLocation[] {
            ResourceLocation.fromNamespaceAndPath(VoxelStandMod.MOD_ID, "speed_penalty_feet"),
            ResourceLocation.fromNamespaceAndPath(VoxelStandMod.MOD_ID, "speed_penalty_legs"),
            ResourceLocation.fromNamespaceAndPath(VoxelStandMod.MOD_ID, "speed_penalty_chest"),
            ResourceLocation.fromNamespaceAndPath(VoxelStandMod.MOD_ID, "speed_penalty_head")
    };

    @SubscribeEvent
    public static void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!VoxelDataHelper.hasCustomKey(stack, "CustomSmithStats")) {
            return;
        }

        CompoundTag rootTag = VoxelDataHelper.getCustomData(stack);
        ItemStatData stats = ItemStatData.fromNBT(rootTag.getCompound("CustomSmithStats"));
        EquipmentSlot slot = event.getSlotType();

        // 1. Оружие и инструменты в главной руке
        if (slot == EquipmentSlot.MAINHAND) {
            float damage = stats.isBroken ? 1.0f : stats.damage;
            float speed = stats.isBroken ? 1.0f : stats.attackSpeed;

            event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                    BASE_ATTACK_DAMAGE_ID, damage, AttributeModifier.Operation.ADD_VALUE));

            event.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(
                    BASE_ATTACK_SPEED_ID, speed - 4.0, AttributeModifier.Operation.ADD_VALUE));
        }

        // 2. Доспехи на теле
        if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            int slotIdx = Math.max(0, Math.min(3, slot.getIndex()));
            if (!stats.isBroken && stats.armor > 0) {
                event.addModifier(Attributes.ARMOR, new AttributeModifier(
                        ARMOR_MODIFIER_IDS[slotIdx], stats.armor, AttributeModifier.Operation.ADD_VALUE));
            }
            if (!stats.isBroken && stats.armorToughness > 0) {
                event.addModifier(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(
                        TOUGHNESS_MODIFIER_IDS[slotIdx], stats.armorToughness, AttributeModifier.Operation.ADD_VALUE));
            }
            if (!stats.isBroken && stats.knockbackResistance > 0) {
                event.addModifier(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(
                        KNOCKBACK_MODIFIER_IDS[slotIdx], stats.knockbackResistance, AttributeModifier.Operation.ADD_VALUE));
            }
            // Штраф к скорости бега от веса брони
            if (stats.movementSpeedPenalty < 0) {
                event.addModifier(Attributes.MOVEMENT_SPEED, new AttributeModifier(
                        SPEED_PENALTY_IDS[slotIdx], stats.movementSpeedPenalty, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack tool = event.getEntity().getMainHandItem();
        if (VoxelDataHelper.hasCustomKey(tool, "CustomSmithStats")) {
            CompoundTag rootTag = VoxelDataHelper.getCustomData(tool);
            ItemStatData stats = ItemStatData.fromNBT(rootTag.getCompound("CustomSmithStats"));
            if (stats.isBroken) {
                event.setNewSpeed(0.2f);
            } else if (stats.miningSpeedModifier > 0) {
                event.setNewSpeed(event.getOriginalSpeed() * stats.miningSpeedModifier);
            }
        }
    }
}
