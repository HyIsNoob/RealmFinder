package com.hyisnoob.realmfinder.common.item;

import com.hyisnoob.realmfinder.core.service.PlatformHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

public class PhotographItem extends Item {

    public interface ClientStampCallback {
        void triggerStamp(boolean carve, InteractionHand hand, UUID snapshotId);
    }

    private static ClientStampCallback clientStampCallback = null;

    public static void setClientStampCallback(ClientStampCallback callback) {
        clientStampCallback = callback;
    }

    public PhotographItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag tag = PlatformHelper.getCustomTag(stack);

        if (!tag.hasUUID("SnapshotId")) {
            return InteractionResultHolder.pass(stack);
        }

        if (level.isClientSide && clientStampCallback != null) {
            // Normal right-click: carve world; Shift + Right-click: don't carve (additive only)
            boolean carve = !player.isShiftKeyDown();
            clientStampCallback.triggerStamp(carve, hand, tag.getUUID("SnapshotId"));
        }

        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        CompoundTag tag = PlatformHelper.getCustomTag(stack);
        if (tag.hasUUID("SnapshotId")) {
            int blockCount = tag.getInt("BlockCount");
            tooltipComponents.add(Component.translatable("tooltip.realmfinder.blocks", blockCount)
                    .withStyle(ChatFormatting.AQUA));
            tooltipComponents.add(Component.translatable("tooltip.realmfinder.place_hint")
                    .withStyle(ChatFormatting.YELLOW));
            tooltipComponents.add(Component.translatable("tooltip.realmfinder.carve_hint")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.realmfinder.empty")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public java.util.Optional<net.minecraft.world.inventory.tooltip.TooltipComponent> getTooltipImage(ItemStack stack) {
        CompoundTag tag = PlatformHelper.getCustomTag(stack);
        if (tag.hasUUID("SnapshotId")) {
            return java.util.Optional.of(new PhotoTooltipData(tag.getUUID("SnapshotId"), tag.getInt("BlockCount"), tag));
        }
        return java.util.Optional.empty();
    }
}
