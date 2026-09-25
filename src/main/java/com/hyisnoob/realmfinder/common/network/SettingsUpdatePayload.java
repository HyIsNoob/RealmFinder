package com.hyisnoob.realmfinder.common.network;

import com.hyisnoob.realmfinder.RealmFinder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SettingsUpdatePayload(String json) implements CustomPacketPayload {
    public static final Type<SettingsUpdatePayload> TYPE = new Type<>(RealmFinder.id("settings_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SettingsUpdatePayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> buf.writeUtf(payload.json(), 2048),
                    buf -> new SettingsUpdatePayload(buf.readUtf(2048)));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
