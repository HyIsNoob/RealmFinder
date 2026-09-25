package com.hyisnoob.realmfinder.common.network;

import com.hyisnoob.realmfinder.RealmFinder;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record StampResultPayload() implements FabricPacket {
    public static final ResourceLocation ID = RealmFinder.id("stamp_result");
    public static final PacketType<StampResultPayload> TYPE = PacketType.create(ID, StampResultPayload::read);
    @Override public PacketType<?> getType() { return TYPE; }
    @Override public void write(FriendlyByteBuf buf) {}
    public static StampResultPayload read(FriendlyByteBuf buf) { return new StampResultPayload(); }
}
