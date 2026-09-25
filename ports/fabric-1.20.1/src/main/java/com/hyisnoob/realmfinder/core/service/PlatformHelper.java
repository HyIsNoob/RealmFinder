package com.hyisnoob.realmfinder.core.service;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Version-specific storage for photograph and album data on Minecraft 1.20.1. */
public final class PlatformHelper {
    private PlatformHelper() {}

    public static CompoundTag getCustomTag(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? new CompoundTag() : tag.copy();
    }

    public static void setCustomTag(ItemStack stack, CompoundTag tag) {
        stack.setTag(tag.copy());
    }

    public static boolean hasCustomTag(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && !tag.isEmpty();
    }
}
