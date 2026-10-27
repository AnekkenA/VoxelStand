package com.voxelstand.block;

import com.voxelstand.VoxelStandMod;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(VoxelStandMod.MOD_ID);

    /** Верстак нарезки пластин */
    public static final DeferredBlock<Block> PLATE_CRAFTER = BLOCKS.register("plate_crafter", PlateCrafterBlock::new);

    /** Единая универсальная стойка кастомизации оружия и брони */
    public static final DeferredBlock<Block> SCULPT_STAND = BLOCKS.register("sculpt_stand", SculptStandBlock::new);

    private ModBlocks() {}
}
