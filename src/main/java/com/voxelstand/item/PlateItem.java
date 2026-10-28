package com.voxelstand.item;

import com.voxelstand.api.VoxelDataHelper;
import com.voxelstand.voxel.ModdedMaterialCatalog;
import com.voxelstand.voxel.VoxelMaterial;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Кузнечная пластина для воксельной ковки (Minecraft 1.21.1 NeoForge).
 * Поддерживает ванильные материалы, а также 50+ модовых материалов и любые предметы из ~800 модов
 * с динамическим считыванием цвета центрального пикселя текстуры.
 *
 * При нарезке создается без тега качества (неисследованная).
 * Для выявления качества игрок берет в левую руку подзорную трубу (Spyglass),
 * в правую пластину и зажимает ПКМ: раз в 20 тиков (1 сек) неисследованная пластина
 * изучается и выпадает под ноги игроку:
 * - 75% Обычная (1.0x) — звук рабочего стола кузнеца
 * - 20% Крепкая (1.15x) — звонкий удар наковальни и нотный блок
 * - 5%  Чистая (1.30x) — звук опыта вместе с кузнечным столом и колокольчиком
 */
public class PlateItem extends Item {
    public static final String NBT_MATERIAL = "MaterialId";
    public static final String NBT_MODDED_MATERIAL = "ModdedMaterialId";
    public static final String NBT_CUSTOM_COLOR = "CustomColor";
    public static final String NBT_SIZE = "PlateSize";
    public static final String NBT_QUALITY = "PlateQuality";

    public PlateItem(Properties properties) {
        super(properties);
    }

    /**
     * Создание неисследованной ванильной пластины при нарезке на верстаке.
     */
    public static ItemStack create(VoxelMaterial material, PlateSize size, int count) {
        ItemStack stack = new ItemStack(ModItems.PLATE.get(), count);
        VoxelDataHelper.updateCustomData(stack, tag -> {
            tag.putByte(NBT_MATERIAL, material.getId());
            tag.putString(NBT_SIZE, size.getName());
            tag.putInt(NBT_CUSTOM_COLOR, material.getRgb());
        });
        return stack;
    }

    /**
     * Создание ванильной пластины с заданным качеством.
     */
    public static ItemStack create(VoxelMaterial material, PlateSize size, PlateQuality quality, int count) {
        ItemStack stack = create(material, size, count);
        if (quality != null) {
            VoxelDataHelper.updateCustomData(stack, tag -> tag.putString(NBT_QUALITY, quality.name()));
        }
        return stack;
    }

    /**
     * Создание модовой пластины (из ~800 модов) с динамическим колориметрическим цветом.
     */
    public static ItemStack createModded(String moddedMaterialId, String displayName, int rgbColor, PlateSize size, @Nullable PlateQuality quality, int count) {
        ItemStack stack = new ItemStack(ModItems.PLATE.get(), count);
        VoxelDataHelper.updateCustomData(stack, tag -> {
            tag.putString(NBT_MODDED_MATERIAL, moddedMaterialId);
            tag.putString("MaterialDisplayName", displayName);
            tag.putInt(NBT_CUSTOM_COLOR, rgbColor);
            tag.putString(NBT_SIZE, size.getName());
            if (quality != null) {
                tag.putString(NBT_QUALITY, quality.name());
            }
        });
        return stack;
    }

    public static VoxelMaterial getMaterial(ItemStack stack) {
        CompoundTag tag = VoxelDataHelper.getCustomData(stack);
        if (tag.contains(NBT_MATERIAL)) {
            return VoxelMaterial.byId(tag.getByte(NBT_MATERIAL));
        }
        return VoxelMaterial.IRON;
    }

    public static boolean isModded(ItemStack stack) {
        CompoundTag tag = VoxelDataHelper.getCustomData(stack);
        return tag.contains(NBT_MODDED_MATERIAL);
    }

    public static String getModdedMaterialId(ItemStack stack) {
        CompoundTag tag = VoxelDataHelper.getCustomData(stack);
        return tag.getString(NBT_MODDED_MATERIAL);
    }

    public static int getPlateColor(ItemStack stack) {
        CompoundTag tag = VoxelDataHelper.getCustomData(stack);
        if (tag.contains(NBT_CUSTOM_COLOR)) {
            return tag.getInt(NBT_CUSTOM_COLOR);
        }
        return getMaterial(stack).getRgb();
    }

    public static PlateSize getSize(ItemStack stack) {
        CompoundTag tag = VoxelDataHelper.getCustomData(stack);
        if (tag.contains(NBT_SIZE)) {
            return PlateSize.fromString(tag.getString(NBT_SIZE));
        }
        return PlateSize.SIZE_8x8;
    }

    public static boolean hasQuality(ItemStack stack) {
        CompoundTag tag = VoxelDataHelper.getCustomData(stack);
        return tag.contains(NBT_QUALITY);
    }

    public static @Nullable PlateQuality getQuality(ItemStack stack) {
        CompoundTag tag = VoxelDataHelper.getCustomData(stack);
        if (tag.contains(NBT_QUALITY)) {
            return PlateQuality.fromString(tag.getString(NBT_QUALITY));
        }
        return null;
    }

    public static void setQuality(ItemStack stack, PlateQuality quality) {
        VoxelDataHelper.updateCustomData(stack, tag -> {
            if (quality != null) {
                tag.putString(NBT_QUALITY, quality.name());
            } else {
                tag.remove(NBT_QUALITY);
            }
        });
    }

    @Override
    public Component getName(ItemStack stack) {
        PlateSize size = getSize(stack);
        PlateQuality quality = getQuality(stack);

        String matName;
        CompoundTag tag = VoxelDataHelper.getCustomData(stack);
        if (tag.contains("MaterialDisplayName")) {
            matName = tag.getString("MaterialDisplayName");
        } else if (isModded(stack)) {
            matName = getModdedMaterialId(stack);
        } else {
            matName = getMaterial(stack).getName();
        }

        if (quality == null) {
            return Component.literal("§7Неизученная пластина: " + matName + " (" + size.getName() + ")");
        } else {
            return Component.literal(quality.getColorCode() + quality.getDisplayName() + " §fпластина: " + matName + " (" + size.getName() + ")");
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        appendHoverTextInternal(stack, tooltip);
    }

    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        appendHoverTextInternal(stack, tooltip);
    }

    private void appendHoverTextInternal(ItemStack stack, List<Component> tooltip) {
        PlateSize size = getSize(stack);
        PlateQuality quality = getQuality(stack);
        int color = getPlateColor(stack);

        if (isModded(stack)) {
            String moddedId = getModdedMaterialId(stack);
            tooltip.add(Component.literal("§bМодовый материал: §f" + moddedId + " §7(размер " + size.getWidth() + "x" + size.getHeight() + ")"));
            tooltip.add(Component.literal(String.format("§8Цвет (RGB): #%06X", (color & 0xFFFFFF))));
        } else {
            VoxelMaterial mat = getMaterial(stack);
            tooltip.add(Component.translatable("tooltip.customsmith.plate", mat.getName(), size.getWidth(), size.getHeight()));
            if (mat.isEmissive()) {
                tooltip.add(Component.literal("§6✨ Светится в темноте"));
            }
        }

        if (quality == null) {
            tooltip.add(Component.literal("§8Качество: §7Не исследовано"));
            tooltip.add(Component.literal("§e✦ Осмотр: §fВозьмите подзорную трубу в левую руку и зажмите ПКМ"));
            tooltip.add(Component.literal("§8Скорость: 1 пластина в секунду (20 тиков)"));
            tooltip.add(Component.literal("§8Шансы: §fОбычная 75% §8| §bКрепкая (+15%) 20% §8| §6Чистая (+30%) 5%"));
        } else if (quality == PlateQuality.NORMAL) {
            tooltip.add(Component.literal("§7Качество: §fОбычное §8(множитель 1.0x)"));
        } else if (quality == PlateQuality.STURDY) {
            tooltip.add(Component.literal("§bКачество: §lКРЕПКОЕ §r§b(+15% ко всем характеристикам, в т.ч. броня и твердость)"));
            tooltip.add(Component.literal("§8" + quality.getDescription()));
        } else if (quality == PlateQuality.PURE) {
            tooltip.add(Component.literal("§6Качество: §lЧИСТОЕ §r§6(+30% ко всем характеристикам, в т.ч. броня и твердость)"));
            tooltip.add(Component.literal("§8" + quality.getDescription()));
        }
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPYGLASS;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand == InteractionHand.MAIN_HAND) {
            ItemStack offhand = player.getOffhandItem();
            if (offhand.is(Items.SPYGLASS)) {
                if (!hasQuality(stack)) {
                    player.startUsingItem(hand);
                    if (level.isClientSide) {
                        player.playSound(SoundEvents.SPYGLASS_USE, 1.0f, 1.0f);
                    }
                    return InteractionResultHolder.consume(stack);
                } else {
                    if (!level.isClientSide) {
                        PlateQuality q = getQuality(stack);
                        player.displayClientMessage(Component.literal("§eПластина уже исследована (" + (q != null ? q.getDisplayName() : "") + ")!"), true);
                    }
                    return InteractionResultHolder.fail(stack);
                }
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && context.getHand() == InteractionHand.MAIN_HAND) {
            if (player.getOffhandItem().is(Items.SPYGLASS) && !hasQuality(context.getItemInHand())) {
                player.startUsingItem(context.getHand());
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (!(livingEntity instanceof Player player)) {
            return;
        }

        if (!player.getOffhandItem().is(Items.SPYGLASS) || hasQuality(stack)) {
            player.stopUsingItem();
            return;
        }

        int elapsed = getUseDuration(stack) - remainingUseDuration;

        // Каждые 20 тиков (ровно 1 секунда) расходуется 1 неисследованная пластина
        if (elapsed > 0 && elapsed % 20 == 0) {
            if (!level.isClientSide) {
                PlateSize size = getSize(stack);
                PlateQuality rolled = PlateQuality.rollFromSpyglass(player.getRandom());

                ItemStack examined;
                if (isModded(stack)) {
                    CompoundTag tag = VoxelDataHelper.getCustomData(stack);
                    String modId = tag.getString(NBT_MODDED_MATERIAL);
                    String dName = tag.getString("MaterialDisplayName");
                    int color = tag.getInt(NBT_CUSTOM_COLOR);
                    examined = PlateItem.createModded(modId, dName, color, size, rolled, 1);
                } else {
                    VoxelMaterial mat = getMaterial(stack);
                    examined = PlateItem.create(mat, size, rolled, 1);
                }

                // Тратим 1 неисследованную пластину из руки
                stack.shrink(1);

                // Выпадение готовой пластины на место, где стоит игрок
                ItemEntity drop = new ItemEntity(level, player.getX(), player.getY() + 0.2, player.getZ(), examined);
                drop.setPickUpDelay(20);
                drop.setDeltaMovement(0, 0.05, 0);
                level.addFreshEntity(drop);

                // Звуковое оформление без спама в чат и без громоздких частиц:
                // Чем звонче звук — тем выше качество!
                if (rolled == PlateQuality.PURE) {
                    // Чистая: звук получения опыта + кузнечный стол + яркий звон колокольчика
                    level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.3f);
                    level.playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 0.85f, 1.2f);
                    level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.85f, 1.8f);
                } else if (rolled == PlateQuality.STURDY) {
                    // Крепкая: звонкий удар наковальни + нотный блок (колокольчик)
                    level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.75f, 1.2f);
                    level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.70f, 1.2f);
                } else {
                    // Обычная: звук работы на кузнечном столе (инструментальщик)
                    level.playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 0.85f, 1.0f);
                    level.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_WORK_TOOLSMITH, SoundSource.PLAYERS, 0.60f, 1.0f);
                }

                if (stack.isEmpty()) {
                    player.stopUsingItem();
                }
            } else {
                player.playSound(SoundEvents.SPYGLASS_USE, 0.5f, 1.3f);
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        if (livingEntity instanceof Player player) {
            player.playSound(SoundEvents.SPYGLASS_STOP_USING, 1.0f, 1.0f);
        }
    }
}
