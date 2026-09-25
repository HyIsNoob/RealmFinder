package com.hyisnoob.realmfinder.common.network;

import com.hyisnoob.realmfinder.RealmFinder;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record UndoPayload() implements FabricPacket {
    public static final ResourceLocation ID = RealmFinder.id("undo");
    public static final PacketType<UndoPayload> TYPE = PacketType.create(ID, UndoPayload::read);
    @Override public PacketType<?> getType() { return TYPE; }
    @Override public void write(FriendlyByteBuf buf) {}
    public static UndoPayload read(FriendlyByteBuf buf) { return new UndoPayload(); }
}
