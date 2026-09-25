package com.hyisnoob.realmfinder.common.item;


import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class CameraItem extends Item {

    public interface ClientCaptureCallback {
        void triggerCapture();
    }

    private static ClientCaptureCallback clientCallback = null;

    public static void setClientCaptureCallback(ClientCaptureCallback callback) {
        clientCallback = callback;
    }

    public CameraItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        capture(level, player, stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null) capture(context.getLevel(), player, context.getItemInHand());
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        capture(player.level(), player, stack);
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    public void capture(Level level, Player player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(this)) return;
        if (level.isClientSide && clientCallback != null) clientCallback.triggerCapture();
        player.getCooldowns().addCooldown(this, 20);
    }

    public static boolean hasBlankPhotograph(Player player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(ModItems.EMPTY_PHOTOGRAPH)) return true;
        }
        return false;
    }
}
