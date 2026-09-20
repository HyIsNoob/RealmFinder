package com.hyisnoob.realmfinder.common.network;

import com.hyisnoob.realmfinder.RealmFinder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/**
 * Sent from Client to Server when the player triggers the camera shutter.
 * Carries the exact client-side camera transform for 1:1 voxel capture alignment.
 */
public record TakePhotoPayload(UUID snapshotId, float fov, float farPlane, double eyeX, double eyeY, double eyeZ, float yaw, float pitch) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TakePhotoPayload> TYPE =
            new CustomPacketPayload.Type<>(RealmFinder.id("take_photo"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TakePhotoPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                UUIDUtil.STREAM_CODEC.encode(buf, payload.snapshotId());
                buf.writeFloat(payload.fov());
                buf.writeFloat(payload.farPlane());
                buf.writeDouble(payload.eyeX());
                buf.writeDouble(payload.eyeY());
                buf.writeDouble(payload.eyeZ());
                buf.writeFloat(payload.yaw());
                buf.writeFloat(payload.pitch());
            },
            buf -> new TakePhotoPayload(
                    UUIDUtil.STREAM_CODEC.decode(buf),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readFloat(),
                    buf.readFloat()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
