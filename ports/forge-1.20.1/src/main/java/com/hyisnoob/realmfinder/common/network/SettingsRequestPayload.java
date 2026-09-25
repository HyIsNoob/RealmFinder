package com.hyisnoob.realmfinder.common.network;

import com.hyisnoob.realmfinder.RealmFinder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record SettingsRequestPayload() {
    public static final ResourceLocation ID = RealmFinder.id("settings_request");
    public void write(FriendlyByteBuf buf) {}
    public static SettingsRequestPayload read(FriendlyByteBuf buf) { return new SettingsRequestPayload(); }
}

