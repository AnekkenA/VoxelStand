package com.voxelstand.client;

import com.voxelstand.VoxelStandMod;
import com.voxelstand.block.entity.ModBlockEntities;
import com.voxelstand.client.gui.SculptStandHudOverlay;
import com.voxelstand.client.render.PlateCrafterRenderer;
import com.voxelstand.client.render.SculptStandRenderer;
import com.voxelstand.client.render.VoxelDelegatingBakedModel;
import com.voxelstand.client.render.VoxelItemRenderer;
import com.voxelstand.item.ModItems;
import com.voxelstand.item.PlateItem;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.Map;

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

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        IClientItemExtensions voxelExtension = new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return VoxelItemRenderer.INSTANCE;
            }
        };

        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof TieredItem || item instanceof ArmorItem ||
                item instanceof ShieldItem || item instanceof BowItem || item instanceof CrossbowItem) {
                event.registerItem(voxelExtension, item);
            }
        }
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        for (Map.Entry<ModelResourceLocation, BakedModel> entry : models.entrySet()) {
            BakedModel original = entry.getValue();
            if (original != null && !(original instanceof VoxelDelegatingBakedModel)) {
                entry.setValue(new VoxelDelegatingBakedModel(original));
            }
        }
    }
}
