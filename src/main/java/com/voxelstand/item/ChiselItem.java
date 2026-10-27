package com.voxelstand.item;

import com.voxelstand.api.VoxelDataHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ChiselItem extends Item {
    public ChiselItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                // Переключение режима симметрии
                boolean current = isSymmetryActive(stack);
                VoxelDataHelper.updateCustomData(stack, tag -> tag.putBoolean("SymmetryActive", !current));
                if (!current) {
                    player.displayClientMessage(Component.translatable("message.customsmith.symmetry_enabled"), true);
                } else {
                    player.displayClientMessage(Component.translatable("message.customsmith.symmetry_disabled"), true);
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return super.use(level, player, hand);
    }

    public static boolean isSymmetryActive(ItemStack stack) {
        return VoxelDataHelper.hasCustomKey(stack, "SymmetryActive") 
                && VoxelDataHelper.getCustomData(stack).getBoolean("SymmetryActive");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        appendHoverTextInternal(stack, tooltip);
    }

    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        appendHoverTextInternal(stack, tooltip);
    }

    private void appendHoverTextInternal(ItemStack stack, List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.customsmith.chisel"));
        if (isSymmetryActive(stack)) {
            tooltip.add(Component.literal("§a[Режим зеркальной симметрии: ВКЛ]"));
        } else {
            tooltip.add(Component.literal("§7[Режим зеркальной симметрии: ВЫКЛ]"));
        }
    }
}
