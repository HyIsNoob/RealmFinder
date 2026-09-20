package com.hyisnoob.realmfinder.common.network;

import com.hyisnoob.realmfinder.common.item.ModItems;
import com.hyisnoob.realmfinder.core.engine.UndoManager;
import com.hyisnoob.realmfinder.core.engine.UndoRecord;
import com.hyisnoob.realmfinder.core.engine.ViewfinderEngine;
import com.hyisnoob.realmfinder.core.math.CameraTransform;
import com.hyisnoob.realmfinder.core.service.PlatformHelper;
import com.hyisnoob.realmfinder.core.snapshot.SnapshotSerializer;
import com.hyisnoob.realmfinder.core.snapshot.WorldSnapshot;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class ModPackets {

    public static void registerCommon() {
        PayloadTypeRegistry.playC2S().register(TakePhotoPayload.TYPE, TakePhotoPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(StampPhotoPayload.TYPE, StampPhotoPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(UndoPayload.TYPE, UndoPayload.STREAM_CODEC);
    }

    public static void registerServerReceivers() {
        // Handle Take Photo
        ServerPlayNetworking.registerGlobalReceiver(TakePhotoPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                ItemStack mainStack = player.getMainHandItem();
                ItemStack offStack = player.getOffhandItem();
                boolean hasCamera = mainStack.is(ModItems.CAMERA) || offStack.is(ModItems.CAMERA);

                if (!hasCamera && !player.isCreative()) {
                    return;
                }

                CameraTransform camera = new CameraTransform(
                        payload.eyeX(), payload.eyeY(), payload.eyeZ(),
                        payload.yaw(), payload.pitch()
                );

                WorldSnapshot snapshot = ViewfinderEngine.capture(
                        player.serverLevel(), camera, payload.fov(),
                        ViewfinderEngine.DEFAULT_ASPECT_RATIO,
                        ViewfinderEngine.DEFAULT_NEAR_PLANE,
                        payload.farPlane(),
                        payload.snapshotId()
                );

                ItemStack photoStack = new ItemStack(ModItems.PHOTOGRAPH);
                CompoundTag tag = SnapshotSerializer.toNbt(snapshot);
                PlatformHelper.setCustomTag(photoStack, tag);

                if (!player.getInventory().add(photoStack)) {
                    player.drop(photoStack, false);
                }

                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 1.0f, 1.4f);
            });
        });

        // Handle Stamp Photo
        ServerPlayNetworking.registerGlobalReceiver(StampPhotoPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                InteractionHand usedHand = null;
                if (player.getMainHandItem().is(ModItems.PHOTOGRAPH)) {
                    usedHand = InteractionHand.MAIN_HAND;
                } else if (player.getOffhandItem().is(ModItems.PHOTOGRAPH)) {
                    usedHand = InteractionHand.OFF_HAND;
                }

                if (usedHand == null) {
                    return;
                }

                ItemStack photoStack = player.getItemInHand(usedHand);
                CompoundTag tag = PlatformHelper.getCustomTag(photoStack);
                WorldSnapshot snapshot = SnapshotSerializer.fromNbt(tag);

                if (snapshot == null) {
                    return;
                }

                CameraTransform targetCamera = new CameraTransform(
                        payload.eyeX(), payload.eyeY(), payload.eyeZ(),
                        payload.yaw(), payload.pitch()
                );

                // Prepare undo record
                UndoRecord undoRecord = new UndoRecord(photoStack);

                // Execute stamp & carve with perspective scale
                ViewfinderEngine.stamp(player.serverLevel(), snapshot, targetCamera, payload.carve(), payload.scale(), undoRecord);

                // Save undo record
                UndoManager.pushUndo(player.getUUID(), undoRecord);

                // Sound & Particle feedback
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.4f);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.8f);

                player.serverLevel().sendParticles(ParticleTypes.FLASH,
                        player.getX(), player.getEyeY(), player.getZ(),
                        1, 0, 0, 0, 0);
                player.serverLevel().sendParticles(ParticleTypes.END_ROD,
                        player.getX(), player.getEyeY(), player.getZ(),
                        12, 0.5, 0.5, 0.5, 0.05);

                if (!player.isCreative()) {
                    photoStack.shrink(1);
                }
            });
        });

        // Handle Undo Packet (e.g. from keybind Z)
        ServerPlayNetworking.registerGlobalReceiver(UndoPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                UndoManager.undo(player);
            });
        });
    }
}
