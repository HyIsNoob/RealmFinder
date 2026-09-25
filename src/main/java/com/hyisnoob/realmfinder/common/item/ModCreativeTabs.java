package com.hyisnoob.realmfinder.common.item;

import com.hyisnoob.realmfinder.RealmFinder;
import com.hyisnoob.realmfinder.common.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModCreativeTabs {
    private static final ResourceKey<CreativeModeTab> REALMFINDER = ResourceKey.create(
            BuiltInRegistries.CREATIVE_MODE_TAB.key(), RealmFinder.id("realmfinder"));

    private ModCreativeTabs() {
    }

    public static void register() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, REALMFINDER,
                FabricItemGroup.builder()
                        .title(Component.translatable("itemGroup.realmfinder"))
                        .icon(() -> new ItemStack(ModItems.CAMERA))
                        .build());

        ItemGroupEvents.modifyEntriesEvent(REALMFINDER).register(entries -> {
            entries.accept(ModItems.CAMERA);
            entries.accept(ModItems.EMPTY_PHOTOGRAPH);
            entries.accept(ModItems.PHOTO_ALBUM);
            entries.accept(ModBlocks.PHOTO_STAND_ITEM);
        });
    }
}
