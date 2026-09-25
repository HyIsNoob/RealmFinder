package com.hyisnoob.realmfinder.common.network;

import com.hyisnoob.realmfinder.RealmFinder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record SettingsSyncPayload(String json, boolean editable) {
    public static final ResourceLocation ID = RealmFinder.id("settings_sync");
    public void write(FriendlyByteBuf buf) { buf.writeUtf(json, 2048); buf.writeBoolean(editable); }
    public static SettingsSyncPayload read(FriendlyByteBuf buf) { return new SettingsSyncPayload(buf.readUtf(2048), buf.readBoolean()); }
}

