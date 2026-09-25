package com.hyisnoob.realmfinder.common.network;

import com.hyisnoob.realmfinder.RealmFinder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SettingsRequestPayload() implements CustomPacketPayload {
    public static final Type<SettingsRequestPayload> TYPE = new Type<>(RealmFinder.id("settings_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SettingsRequestPayload> STREAM_CODEC =
            StreamCodec.unit(new SettingsRequestPayload());

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
