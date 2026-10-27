package com.voxelstand.event;

import com.voxelstand.VoxelStandMod;
import com.voxelstand.api.VoxelDataHelper;
import com.voxelstand.voxel.VoxelMaterial;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.ShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import org.joml.Vector3f;

@EventBusSubscriber(modid = VoxelStandMod.MOD_ID)
public class ParticleCombatHandler {

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();
        if (player.level().isClientSide || target == null) return;

        ItemStack weapon = player.getMainHandItem();
        if (!VoxelDataHelper.hasCustomKey(weapon, "CustomSmithStats")) return;

        ServerLevel serverLevel = (ServerLevel) player.level();
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.5;
        double z = target.getZ();

        byte domMat = VoxelDataHelper.getCustomData(weapon).getCompound("CustomSmithStats").getByte("DominantMat");
        VoxelMaterial mat = VoxelMaterial.byId(domMat);

        switch (mat) {
            case BLAZE -> {
                serverLevel.sendParticles(ParticleTypes.FLAME, x, y, z, 15, 0.3, 0.3, 0.3, 0.05);
                serverLevel.sendParticles(ParticleTypes.LAVA, x, y, z, 5, 0.2, 0.2, 0.2, 0.02);
            }
            case AMETHYST -> {
                serverLevel.sendParticles(ParticleTypes.AMETHYST_RESONATE, x, y, z, 12, 0.4, 0.4, 0.4, 0.05);
            }
            case ECHO_SHARD -> {
                serverLevel.sendParticles(ParticleTypes.SCULK_SOUL, x, y, z, 8, 0.3, 0.3, 0.3, 0.03);
            }
            case REDSTONE -> {
                serverLevel.sendParticles(new DustParticleOptions(new Vector3f(1.0f, 0.0f, 0.0f), 1.0f),
                        x, y, z, 15, 0.3, 0.3, 0.3, 0.05);
            }
            case GOLD -> {
                serverLevel.sendParticles(ParticleTypes.WAX_OFF, x, y, z, 10, 0.3, 0.3, 0.3, 0.05);
            }
            case SEA_LANTERN -> {
                serverLevel.sendParticles(ParticleTypes.GLOW, x, y, z, 12, 0.3, 0.3, 0.3, 0.05);
            }
            default -> {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 8, 0.2, 0.2, 0.2, 0.05);
                serverLevel.sendParticles(ParticleTypes.CRIT, x, y, z, 6, 0.2, 0.2, 0.2, 0.05);
            }
        }
    }

    @SubscribeEvent
    public static void onShieldBlock(ShieldBlockEvent event) {
        if (event.getEntity().level().isClientSide) return;

        Entity blocker = event.getEntity();
        ServerLevel serverLevel = (ServerLevel) blocker.level();
        double x = blocker.getX();
        double y = blocker.getY() + 1.0;
        double z = blocker.getZ();

        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 12, 0.3, 0.3, 0.3, 0.08);
    }

    @SubscribeEvent
    public static void onUseItemTick(LivingEntityUseItemEvent.Tick event) {
        if (event.getEntity() instanceof Player player && player.level().isClientSide) {
            ItemStack stack = event.getItem();
            int duration = event.getDuration();
            int maxDuration = stack.getUseDuration(player);
            int chargeTicks = maxDuration - duration;

            if (stack.getItem() instanceof BowItem && chargeTicks == 20) {
                player.playSound(SoundEvents.ARROW_HIT_PLAYER, 0.7f, 1.8f);
            } else if (stack.getItem() instanceof CrossbowItem) {
                int chargeDuration = CrossbowItem.getChargeDuration(stack, player);
                if (chargeTicks == chargeDuration) {
                    player.playSound(SoundEvents.CROSSBOW_LOADING_END, 0.9f, 1.2f);
                }
            }
        }
    }
}
