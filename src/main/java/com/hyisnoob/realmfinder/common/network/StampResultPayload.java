package com.hyisnoob.realmfinder.common.network;

import com.hyisnoob.realmfinder.RealmFinder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record StampResultPayload() implements CustomPacketPayload {
    public static final Type<StampResultPayload> TYPE = new Type<>(RealmFinder.id("stamp_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StampResultPayload> STREAM_CODEC =
            StreamCodec.unit(new StampResultPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
