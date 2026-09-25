package com.hyisnoob.realmfinder.common.network;

import com.hyisnoob.realmfinder.RealmFinder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record StampPhotoPayload(boolean carve, boolean offHand, UUID snapshotId,
                                double eyeX, double eyeY, double eyeZ, float yaw, float pitch, float scale) {
    public static final ResourceLocation ID = RealmFinder.id("stamp_photo");

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(carve);
        buf.writeBoolean(offHand);
        buf.writeUUID(snapshotId);
        buf.writeDouble(eyeX);
        buf.writeDouble(eyeY);
        buf.writeDouble(eyeZ);
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
        buf.writeFloat(scale);
    }

    public static StampPhotoPayload read(FriendlyByteBuf buf) {
        return new StampPhotoPayload(buf.readBoolean(), buf.readBoolean(), buf.readUUID(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readFloat(), buf.readFloat());
    }
}

