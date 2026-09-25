package com.hyisnoob.realmfinder.common.network;

import com.hyisnoob.realmfinder.RealmFinder;
import com.hyisnoob.realmfinder.core.config.RealmFinderConfig;
import com.hyisnoob.realmfinder.common.item.ModItems;
import com.hyisnoob.realmfinder.core.engine.UndoManager;
import com.hyisnoob.realmfinder.core.engine.UndoRecord;
import com.hyisnoob.realmfinder.core.engine.GameplayLimits;
import com.hyisnoob.realmfinder.core.engine.ActionThrottle;
import com.hyisnoob.realmfinder.core.engine.ViewfinderEngine;
import com.hyisnoob.realmfinder.core.math.CameraTransform;
import com.hyisnoob.realmfinder.core.service.PlatformHelper;
import com.hyisnoob.realmfinder.core.snapshot.SnapshotSerializer;
import com.hyisnoob.realmfinder.core.snapshot.WorldSnapshot;
import com.hyisnoob.realmfinder.core.snapshot.PhotoThumbnail;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class ModPackets {
    private static final ActionThrottle CAPTURE_THROTTLE = new ActionThrottle();
    private static final ActionThrottle STAMP_THROTTLE = new ActionThrottle();
    private static final ActionThrottle UNDO_THROTTLE = new ActionThrottle();

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(TakePhotoPayload.TYPE, TakePhotoPayload.STREAM_CODEC, ModPackets::takePhoto);
        registrar.playToServer(StampPhotoPayload.TYPE, StampPhotoPayload.STREAM_CODEC, ModPackets::stampPhoto);
        registrar.playToServer(UndoPayload.TYPE, UndoPayload.STREAM_CODEC, ModPackets::undo);
        registrar.playToServer(SettingsRequestPayload.TYPE, SettingsRequestPayload.STREAM_CODEC, ModPackets::requestSettings);
        registrar.playToServer(SettingsUpdatePayload.TYPE, SettingsUpdatePayload.STREAM_CODEC, ModPackets::updateSettings);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            registrar.playToClient(StampResultPayload.TYPE, StampResultPayload.STREAM_CODEC,
                    com.hyisnoob.realmfinder.client.RealmFinderClient::receiveStampResult);
            registrar.playToClient(SettingsSyncPayload.TYPE, SettingsSyncPayload.STREAM_CODEC,
                    com.hyisnoob.realmfinder.client.RealmFinderClient::receiveSettings);
        } else {
            registrar.playToClient(StampResultPayload.TYPE, StampResultPayload.STREAM_CODEC,
                    (payload, context) -> {});
            registrar.playToClient(SettingsSyncPayload.TYPE, SettingsSyncPayload.STREAM_CODEC,
                    (payload, context) -> {});
        }
    }

    public static void registerServerEvents() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) sendSettings(player);
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            CAPTURE_THROTTLE.clear(player.getUUID());
            STAMP_THROTTLE.clear(player.getUUID());
            UNDO_THROTTLE.clear(player.getUUID());
        });
    }

    private static void requestSettings(SettingsRequestPayload payload, IPayloadContext neoContext) {
        Context context = new Context(neoContext);
        context.server().execute(() -> sendSettings(context.player()));
    }

    private static void updateSettings(SettingsUpdatePayload payload, IPayloadContext neoContext) {
        Context context = new Context(neoContext);
        context.server().execute(() -> {
                    ServerPlayer editor = context.player();
                    if (!mayEditSettings(editor)) {
                        sendSettings(editor);
                        return;
                    }
                    try {
                        RealmFinderConfig.update(RealmFinderConfig.fromJson(payload.json()));
                        UndoManager.enforceLimit();
                        for (ServerPlayer viewer : context.server().getPlayerList().getPlayers()) sendSettings(viewer);
                    } catch (RuntimeException e) {
                        RealmFinder.LOGGER.warn("Rejected RealmFinder settings update", e);
                        sendSettings(editor);
                    }
        });
    }

    private static void takePhoto(TakePhotoPayload payload, IPayloadContext neoContext) {
        Context context = new Context(neoContext);
        // Handle Take Photo
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                ItemStack mainStack = player.getMainHandItem();
                ItemStack offStack = player.getOffhandItem();
                boolean hasCamera = mainStack.is(ModItems.CAMERA) || offStack.is(ModItems.CAMERA);
                ItemStack cameraStack = mainStack.is(ModItems.CAMERA) ? mainStack : offStack;

                if (!hasCamera) {
                    return;
                }
                var rules = RealmFinderConfig.get();
                if (rules.damagesCamera(player.isCreative()) && cameraStack.getDamageValue() >= cameraStack.getMaxDamage()) {
                    player.displayClientMessage(Component.translatable("message.realmfinder.camera_worn"), true);
                    return;
                }
                if (rules.needsBlank(player.isCreative())
                        && !com.hyisnoob.realmfinder.common.item.CameraItem.hasBlankPhotograph(player)) {
                    player.displayClientMessage(Component.translatable("message.realmfinder.blank_missing"), true);
                    return;
                }
                if (!GameplayLimits.validCapture(payload.fov(), payload.farPlane(),
                        payload.eyeX(), payload.eyeY(), payload.eyeZ(), payload.yaw(), payload.pitch(),
                        player.getX(), player.getEyeY(), player.getZ())) return;
                if (payload.cameraZoom() < 1 || payload.cameraZoom() > 6) return;
                if (!CAPTURE_THROTTLE.allow(player.getUUID(), context.server().getTickCount(), 20)) return;

                CameraTransform camera = new CameraTransform(
                        payload.eyeX(), payload.eyeY(), payload.eyeZ(),
                        payload.yaw(), payload.pitch()
                );

                WorldSnapshot snapshot;
                try {
                    snapshot = ViewfinderEngine.capture(
                            player.serverLevel(), camera, payload.fov(),
                            ViewfinderEngine.DEFAULT_ASPECT_RATIO,
                            ViewfinderEngine.DEFAULT_NEAR_PLANE,
                            payload.farPlane(),
                            payload.snapshotId(), payload.cameraZoom()
                    );
                } catch (RuntimeException e) {
                    RealmFinder.LOGGER.error("Photograph capture failed", e);
                    player.displayClientMessage(Component.translatable("message.realmfinder.capture_rejected"), true);
                    return;
                }
                if (snapshot == null || (snapshot.getBlockCount() == 0 && snapshot.getEntityCount() == 0)) {
                    player.displayClientMessage(Component.translatable("message.realmfinder.capture_rejected"), true);
                    return;
                }

                ItemStack photoStack = new ItemStack(ModItems.PHOTOGRAPH);
                CompoundTag tag;
                try {
                    tag = SnapshotSerializer.toNbt(snapshot);
                } catch (RuntimeException e) {
                    RealmFinder.LOGGER.error("Photograph serialization failed", e);
                    player.displayClientMessage(Component.translatable("message.realmfinder.capture_rejected"), true);
                    return;
                }
                if (tag == null || tag.getByteArray("CompressedBlocks").length > GameplayLimits.MAX_COMPRESSED_BLOCK_BYTES) {
                    player.displayClientMessage(Component.translatable("message.realmfinder.capture_rejected"), true);
                    return;
                }
                if (PhotoThumbnail.isValid(payload.thumbnail())) {
                    tag.putByteArray("Thumbnail", payload.thumbnail());
                }
                PlatformHelper.setCustomTag(photoStack, tag);

                if (rules.needsBlank(player.isCreative())) {
                    int blankSlot = -1;
                    for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                        if (player.getInventory().getItem(slot).is(ModItems.EMPTY_PHOTOGRAPH)) {
                            blankSlot = slot;
                            break;
                        }
                    }
                    if (blankSlot < 0) return;
                    player.getInventory().getItem(blankSlot).shrink(1);
                }
                if (rules.damagesCamera(player.isCreative())) {
                    int nextDamage = cameraStack.getDamageValue() + 1;
                    if (nextDamage >= cameraStack.getMaxDamage()) cameraStack.shrink(1);
                    else cameraStack.setDamageValue(nextDamage);
                }

                if (!player.getInventory().add(photoStack)) {
                    player.drop(photoStack, false);
                }

                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 1.0f, 1.4f);
            });
    }

    private static void stampPhoto(StampPhotoPayload payload, IPayloadContext neoContext) {
        Context context = new Context(neoContext);
        // Handle Stamp Photo
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                if (!GameplayLimits.validStamp(payload.scale(), payload.eyeX(), payload.eyeY(), payload.eyeZ(),
                        payload.yaw(), payload.pitch(), player.getX(), player.getEyeY(), player.getZ())) return;
                if (!STAMP_THROTTLE.allow(player.getUUID(), context.server().getTickCount(), 10)) return;
                InteractionHand usedHand = payload.offHand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;

                ItemStack photoStack = player.getItemInHand(usedHand);
                if (!photoStack.is(ModItems.PHOTOGRAPH)) return;
                CompoundTag tag = PlatformHelper.getCustomTag(photoStack);
                if (!tag.hasUUID("SnapshotId") || !payload.snapshotId().equals(tag.getUUID("SnapshotId"))) return;
                WorldSnapshot snapshot;
                try {
                    snapshot = SnapshotSerializer.fromNbt(tag);
                } catch (RuntimeException e) {
                    RealmFinder.LOGGER.warn("Rejected malformed Photograph", e);
                    player.displayClientMessage(Component.translatable("message.realmfinder.stamp_rejected"), true);
                    return;
                }

                if (snapshot == null) {
                    player.displayClientMessage(Component.translatable("message.realmfinder.stamp_rejected"), true);
                    return;
                }

                CameraTransform targetCamera = new CameraTransform(
                        payload.eyeX(), payload.eyeY(), payload.eyeZ(),
                        payload.yaw(), payload.pitch()
                );

                // Prepare undo record
                UndoRecord undoRecord = new UndoRecord(photoStack, !player.isCreative());

                // Execute stamp & carve with perspective scale
                boolean placed;
                try {
                    placed = ViewfinderEngine.stamp(player.serverLevel(), snapshot, targetCamera,
                            payload.carve(), payload.scale(), undoRecord);
                } catch (RuntimeException e) {
                    RealmFinder.LOGGER.error("Photograph placement preflight failed", e);
                    player.displayClientMessage(Component.translatable("message.realmfinder.stamp_rejected"), true);
                    return;
                }
                if (!placed) {
                    player.displayClientMessage(Component.translatable("message.realmfinder.stamp_rejected"), true);
                    return;
                }

                // Save undo record
                try {
                    undoRecord.seal(player.serverLevel());
                } catch (RuntimeException e) {
                    RealmFinder.LOGGER.error("Could not save photograph undo state", e);
                    try {
                        undoRecord.restore(player.serverLevel(), null);
                    } catch (RuntimeException rollbackError) {
                        RealmFinder.LOGGER.error("Could not roll back photograph placement", rollbackError);
                    }
                    player.displayClientMessage(Component.translatable("message.realmfinder.stamp_rejected"), true);
                    return;
                }
                UndoManager.pushUndo(player.getUUID(), undoRecord);
                PacketDistributor.sendToPlayer(player, new StampResultPayload());

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
    }

    private static void undo(UndoPayload payload, IPayloadContext neoContext) {
        Context context = new Context(neoContext);
        // Handle Undo Packet (e.g. from keybind Z)
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                if (!UNDO_THROTTLE.allow(player.getUUID(), context.server().getTickCount(), 10)) return;
                UndoManager.undo(player);
            });
    }

    private record Context(IPayloadContext delegate) {
        ServerPlayer player() { return (ServerPlayer) delegate.player(); }
        net.minecraft.server.MinecraftServer server() { return player().getServer(); }
    }

    private static void sendSettings(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SettingsSyncPayload(
                RealmFinderConfig.toJson(RealmFinderConfig.get()), mayEditSettings(player)));
    }

    private static boolean mayEditSettings(ServerPlayer player) {
        return player.hasPermissions(2) || player.getServer().isSingleplayerOwner(player.getGameProfile());
    }
}
