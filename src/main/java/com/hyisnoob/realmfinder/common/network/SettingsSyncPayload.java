package com.hyisnoob.realmfinder.common.network;

import com.hyisnoob.realmfinder.RealmFinder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SettingsSyncPayload(String json, boolean editable) implements CustomPacketPayload {
    public static final Type<SettingsSyncPayload> TYPE = new Type<>(RealmFinder.id("settings_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SettingsSyncPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> { buf.writeUtf(payload.json(), 2048); buf.writeBoolean(payload.editable()); },
                    buf -> new SettingsSyncPayload(buf.readUtf(2048), buf.readBoolean()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
