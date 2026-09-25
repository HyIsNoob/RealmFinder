package com.hyisnoob.realmfinder.common.network;

import com.hyisnoob.realmfinder.RealmFinder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record UndoPayload() {
    public static final ResourceLocation ID = RealmFinder.id("undo");
    public void write(FriendlyByteBuf buf) {}
    public static UndoPayload read(FriendlyByteBuf buf) { return new UndoPayload(); }
}

