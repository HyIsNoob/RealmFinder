package com.hyisnoob.realmfinder.common.network;

import com.hyisnoob.realmfinder.RealmFinder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record StampResultPayload() {
    public static final ResourceLocation ID = RealmFinder.id("stamp_result");
    public void write(FriendlyByteBuf buf) {}
    public static StampResultPayload read(FriendlyByteBuf buf) { return new StampResultPayload(); }
}

