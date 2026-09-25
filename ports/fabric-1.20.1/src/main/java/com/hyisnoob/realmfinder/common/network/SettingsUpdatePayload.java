package com.hyisnoob.realmfinder.common.network;

import com.hyisnoob.realmfinder.RealmFinder;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record SettingsUpdatePayload(String json) implements FabricPacket {
    public static final ResourceLocation ID = RealmFinder.id("settings_update");
    public static final PacketType<SettingsUpdatePayload> TYPE = PacketType.create(ID, SettingsUpdatePayload::read);
    @Override public PacketType<?> getType() { return TYPE; }
    @Override public void write(FriendlyByteBuf buf) { buf.writeUtf(json, 2048); }
    public static SettingsUpdatePayload read(FriendlyByteBuf buf) { return new SettingsUpdatePayload(buf.readUtf(2048)); }
}
