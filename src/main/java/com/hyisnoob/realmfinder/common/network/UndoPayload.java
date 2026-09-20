package com.hyisnoob.realmfinder.common.network;

import com.hyisnoob.realmfinder.RealmFinder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record UndoPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UndoPayload> TYPE =
            new CustomPacketPayload.Type<>(RealmFinder.id("undo"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UndoPayload> STREAM_CODEC =
            StreamCodec.unit(new UndoPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
