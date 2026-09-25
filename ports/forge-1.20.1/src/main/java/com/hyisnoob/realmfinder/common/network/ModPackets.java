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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.network.FriendlyByteBuf;
import java.util.function.BiConsumer;
import java.util.function.Function;
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
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            RealmFinder.id("main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static int nextPacketId;

    public static void registerCommon() {
        registerS2C(StampResultPayload.class, StampResultPayload::write, StampResultPayload::read,
                payload -> {
                    if (FMLEnvironment.dist == Dist.CLIENT) {
                        com.hyisnoob.realmfinder.client.RealmFinderClient.receiveStampResult();
                    }
                });
        registerS2C(SettingsSyncPayload.class, SettingsSyncPayload::write, SettingsSyncPayload::read,
                payload -> {
                    if (FMLEnvironment.dist == Dist.CLIENT) {
                        com.hyisnoob.realmfinder.client.RealmFinderClient.receiveSettings(payload);
                    }
                });
    }

    public static void registerServerReceivers() {
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) sendSettings(player);
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            CAPTURE_THROTTLE.clear(player.getUUID());
            STAMP_THROTTLE.clear(player.getUUID());
            UNDO_THROTTLE.clear(player.getUUID());
        });
        registerC2S(SettingsRequestPayload.class, SettingsRequestPayload::write, SettingsRequestPayload::read, (payload, player) ->
                player.getServer().execute(() -> sendSettings(player)));
        registerC2S(SettingsUpdatePayload.class, SettingsUpdatePayload::write, SettingsUpdatePayload::read, (payload, player) ->
                player.getServer().execute(() -> {
                    ServerPlayer editor = player;
                    if (!mayEditSettings(editor)) {
                        sendSettings(editor);
                        return;
                    }
                    try {
                        RealmFinderConfig.update(RealmFinderConfig.fromJson(payload.json()));
                        UndoManager.enforceLimit();
                        for (ServerPlayer viewer : player.getServer().getPlayerList().getPlayers()) sendSettings(viewer);
                    } catch (RuntimeException e) {
                        RealmFinder.LOGGER.warn("Rejected RealmFinder settings update", e);
                        sendSettings(editor);
                    }
                }));
        // Handle Take Photo
        registerC2S(TakePhotoPayload.class, TakePhotoPayload::write, TakePhotoPayload::read, (payload, player) -> {
            player.getServer().execute(() -> {
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
                if (!CAPTURE_THROTTLE.allow(player.getUUID(), player.getServer().getTickCount(), 20)) return;

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
        });

        // Handle Stamp Photo
        registerC2S(StampPhotoPayload.class, StampPhotoPayload::write, StampPhotoPayload::read, (payload, player) -> {
            player.getServer().execute(() -> {
                if (!GameplayLimits.validStamp(payload.scale(), payload.eyeX(), payload.eyeY(), payload.eyeZ(),
                        payload.yaw(), payload.pitch(), player.getX(), player.getEyeY(), player.getZ())) return;
                if (!STAMP_THROTTLE.allow(player.getUUID(), player.getServer().getTickCount(), 10)) return;
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
                sendToClient(player, new StampResultPayload());

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
        registerC2S(UndoPayload.class, UndoPayload::write, UndoPayload::read, (payload, player) -> {
            player.getServer().execute(() -> {
                if (!UNDO_THROTTLE.allow(player.getUUID(), player.getServer().getTickCount(), 10)) return;
                UndoManager.undo(player);
            });
        });
    }

    private static void sendSettings(ServerPlayer player) {
        sendToClient(player, new SettingsSyncPayload(
                RealmFinderConfig.toJson(RealmFinderConfig.get()), mayEditSettings(player)));
    }

    private static boolean mayEditSettings(ServerPlayer player) {
        return player.hasPermissions(2) || player.getServer().isSingleplayerOwner(player.getGameProfile());
    }

    private static <T> void registerC2S(Class<T> type, BiConsumer<T, FriendlyByteBuf> writer,
                                        Function<FriendlyByteBuf, T> reader, BiConsumer<T, ServerPlayer> handler) {
        CHANNEL.registerMessage(nextPacketId++, type, writer, reader, (payload, supplier) -> {
            var context = supplier.get();
            ServerPlayer player = context.getSender();
            if (player != null) context.enqueueWork(() -> handler.accept(payload, player));
            context.setPacketHandled(true);
        });
    }

    private static <T> void registerS2C(Class<T> type, BiConsumer<T, FriendlyByteBuf> writer,
                                        Function<FriendlyByteBuf, T> reader, java.util.function.Consumer<T> handler) {
        CHANNEL.registerMessage(nextPacketId++, type, writer, reader, (payload, supplier) -> {
            var context = supplier.get();
            context.enqueueWork(() -> handler.accept(payload));
            context.setPacketHandled(true);
        });
    }

    public static void sendToServer(Object payload) { CHANNEL.sendToServer(payload); }
    private static void sendToClient(ServerPlayer player, Object payload) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }
}
