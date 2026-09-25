package com.hyisnoob.realmfinder.common.network;

import com.hyisnoob.realmfinder.RealmFinder;
import com.hyisnoob.realmfinder.core.snapshot.PhotoThumbnail;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record TakePhotoPayload(UUID snapshotId, float fov, float farPlane, int cameraZoom,
                               double eyeX, double eyeY, double eyeZ, float yaw, float pitch, byte[] thumbnail) {
    public static final ResourceLocation ID = RealmFinder.id("take_photo");

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(snapshotId);
        buf.writeFloat(fov);
        buf.writeFloat(farPlane);
        buf.writeVarInt(cameraZoom);
        buf.writeDouble(eyeX);
        buf.writeDouble(eyeY);
        buf.writeDouble(eyeZ);
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
        buf.writeByteArray(thumbnail);
    }

    public static TakePhotoPayload read(FriendlyByteBuf buf) {
        return new TakePhotoPayload(buf.readUUID(), buf.readFloat(), buf.readFloat(), buf.readVarInt(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readFloat(),
                buf.readByteArray(PhotoThumbnail.MAX_BYTES));
    }
}

