package com.hyisnoob.realmfinder.common.network;

import com.hyisnoob.realmfinder.RealmFinder;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record SettingsRequestPayload() implements FabricPacket {
    public static final ResourceLocation ID = RealmFinder.id("settings_request");
    public static final PacketType<SettingsRequestPayload> TYPE = PacketType.create(ID, SettingsRequestPayload::read);
    @Override public PacketType<?> getType() { return TYPE; }
    @Override public void write(FriendlyByteBuf buf) {}
    public static SettingsRequestPayload read(FriendlyByteBuf buf) { return new SettingsRequestPayload(); }
}
