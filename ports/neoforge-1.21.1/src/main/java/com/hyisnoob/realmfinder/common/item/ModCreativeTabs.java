package com.hyisnoob.realmfinder.common.item;

import com.hyisnoob.realmfinder.RealmFinder;
import com.hyisnoob.realmfinder.common.block.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.RegisterEvent;

public final class ModCreativeTabs {
    private static final ResourceKey<CreativeModeTab> REALMFINDER = ResourceKey.create(
            BuiltInRegistries.CREATIVE_MODE_TAB.key(), RealmFinder.id("realmfinder"));

    private ModCreativeTabs() {
    }

    public static void register(RegisterEvent event) {
        event.register(BuiltInRegistries.CREATIVE_MODE_TAB.key(), helper -> {
            helper.register(RealmFinder.id("realmfinder"), CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.realmfinder"))
                    .icon(() -> new ItemStack(ModItems.CAMERA))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.CAMERA);
                        output.accept(ModItems.EMPTY_PHOTOGRAPH);
                        output.accept(ModItems.PHOTO_ALBUM);
                        output.accept(ModBlocks.PHOTO_STAND_ITEM);
                    }).build());
        });
    }
}
