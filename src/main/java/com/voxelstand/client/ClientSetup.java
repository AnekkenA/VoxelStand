package com.voxelstand.client;

import com.voxelstand.VoxelStandMod;
import com.voxelstand.block.entity.ModBlockEntities;
import com.voxelstand.client.render.PlateCrafterRenderer;
import com.voxelstand.client.render.SculptStandRenderer;
import com.voxelstand.item.ModItems;
import com.voxelstand.item.PlateItem;
import com.voxelstand.client.gui.SculptStandHudOverlay;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = VoxelStandMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Регистрация единого рендерера для универсальной стойки
        event.registerBlockEntityRenderer(ModBlockEntities.SCULPT_STAND.get(), SculptStandRenderer::new);
        // Регистрация рендерера вращающегося маховика нарезчика пластин
        event.registerBlockEntityRenderer(ModBlockEntities.PLATE_CRAFTER.get(), PlateCrafterRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.AddLayers event) {
        // Добавление слоя воксельной кастомной брони для всех моделей игроков (default и slim)
        for (net.minecraft.client.resources.PlayerSkin.Model skin : event.getSkins()) {
            net.minecraft.client.renderer.entity.player.PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new com.voxelstand.client.render.CustomArmorLayer<>(renderer));
            }
        }
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        // Динамическая окраска пластин в точный RGB-цвет материала (ванильного или модового из ~800 модов)
        event.register((stack, tintIndex) -> {
            if (tintIndex == 0) {
                return PlateItem.getPlateColor(stack);
            }
            return 0xFFFFFF;
        }, ModItems.PLATE.get());
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        // Регистрация HUD-оверлея стапеля поверх прицела
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(VoxelStandMod.MOD_ID, "sculpt_stand_hud"), new SculptStandHudOverlay());
    }
}
