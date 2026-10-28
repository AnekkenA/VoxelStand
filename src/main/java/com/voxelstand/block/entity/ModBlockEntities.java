package com.voxelstand.block.entity;

import com.voxelstand.VoxelStandMod;
import com.voxelstand.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, VoxelStandMod.MOD_ID);

    /** Единый BlockEntityType для универсальной стойки */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SculptStandBlockEntity>> SCULPT_STAND =
            BLOCK_ENTITIES.register("sculpt_stand", () ->
                    BlockEntityType.Builder.of(SculptStandBlockEntity::new, ModBlocks.SCULPT_STAND.get()).build(null));

    /** BlockEntityType для нарезчика пластин с анимированным маховиком */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlateCrafterBlockEntity>> PLATE_CRAFTER =
            BLOCK_ENTITIES.register("plate_crafter", () ->
                    BlockEntityType.Builder.of(PlateCrafterBlockEntity::new, ModBlocks.PLATE_CRAFTER.get()).build(null));

    /** BlockEntityType для прокси-блока мультиблока */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StandPartBlockEntity>> STAND_PART =
            BLOCK_ENTITIES.register("stand_part", () ->
                    BlockEntityType.Builder.of(StandPartBlockEntity::new, ModBlocks.STAND_PART.get()).build(null));

    private ModBlockEntities() {}
}
