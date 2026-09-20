package com.hyisnoob.realmfinder.common.network;

import com.hyisnoob.realmfinder.RealmFinder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent from Client to Server when placing/stamping the photo into the world.
 * Carries the exact client-side placement transform for 1:1 voxel alignment.
 */
public record StampPhotoPayload(boolean carve, double eyeX, double eyeY, double eyeZ, float yaw, float pitch, float scale) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<StampPhotoPayload> TYPE =
            new CustomPacketPayload.Type<>(RealmFinder.id("stamp_photo"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StampPhotoPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBoolean(payload.carve());
                buf.writeDouble(payload.eyeX());
                buf.writeDouble(payload.eyeY());
                buf.writeDouble(payload.eyeZ());
                buf.writeFloat(payload.yaw());
                buf.writeFloat(payload.pitch());
                buf.writeFloat(payload.scale());
            },
            buf -> new StampPhotoPayload(
                    buf.readBoolean(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
