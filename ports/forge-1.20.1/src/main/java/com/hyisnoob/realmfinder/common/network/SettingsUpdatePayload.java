package com.hyisnoob.realmfinder.common.network;

import com.hyisnoob.realmfinder.RealmFinder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record SettingsUpdatePayload(String json) {
    public static final ResourceLocation ID = RealmFinder.id("settings_update");
    public void write(FriendlyByteBuf buf) { buf.writeUtf(json, 2048); }
    public static SettingsUpdatePayload read(FriendlyByteBuf buf) { return new SettingsUpdatePayload(buf.readUtf(2048)); }
}

