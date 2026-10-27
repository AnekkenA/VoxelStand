package com.voxelstand;

import com.voxelstand.block.ModBlocks;
import com.voxelstand.block.entity.ModBlockEntities;
import com.voxelstand.command.VoxelStandCommands;
import com.voxelstand.item.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Voxel Stand — главный мод воксельной ковки оружия и брони (Minecraft 1.21.1 NeoForge).
 * Полностью автономен, совместим со сборками на ~800+ модов.
 */
@Mod(VoxelStandMod.MOD_ID)
public class VoxelStandMod {
    public static final String MOD_ID = "voxelstand";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public VoxelStandMod(IEventBus modEventBus) {
        // Регистрация контента мода на шине мода
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModItems.CREATIVE_MODE_TABS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
        LOGGER.info("Voxel Stand Mod 1.21.1 NeoForge initialized successfully!");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        VoxelStandCommands.register(event.getDispatcher());
    }
}
