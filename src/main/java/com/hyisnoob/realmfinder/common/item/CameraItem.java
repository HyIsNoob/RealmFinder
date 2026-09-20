package com.hyisnoob.realmfinder.common.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

        if (level.isClientSide && clientCallback != null) {
            clientCallback.triggerCapture();
        }

        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
