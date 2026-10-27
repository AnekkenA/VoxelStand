package com.voxelstand.item;

import com.voxelstand.VoxelStandMod;
import com.voxelstand.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VoxelStandMod.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, VoxelStandMod.MOD_ID);

    public static final DeferredItem<Item> CHISEL = ITEMS.register("chisel",
            () -> new ChiselItem(new Item.Properties()));

    public static final DeferredItem<Item> PLATE = ITEMS.register("plate",
            () -> new PlateItem(new Item.Properties()));

    // Блок-предметы
    public static final DeferredItem<BlockItem> PLATE_CRAFTER_ITEM = ITEMS.register("plate_crafter",
            () -> new BlockItem(ModBlocks.PLATE_CRAFTER.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> SCULPT_STAND_ITEM = ITEMS.register("sculpt_stand",
            () -> new BlockItem(ModBlocks.SCULPT_STAND.get(), new Item.Properties()));

    // Вкладка креатива
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_MODE_TABS.register("customsmith_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.customsmith"))
                    .icon(() -> new ItemStack(CHISEL.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(CHISEL.get());
                        output.accept(PLATE.get());
                        output.accept(PLATE_CRAFTER_ITEM.get());
                        output.accept(SCULPT_STAND_ITEM.get());
                    })
                    .build());

    private ModItems() {}
}
