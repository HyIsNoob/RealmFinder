package com.hyisnoob.realmfinder.common.menu;

import com.hyisnoob.realmfinder.RealmFinder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.RegisterEvent;

public class ModMenus {

    public static MenuType<PhotoAlbumMenu> PHOTO_ALBUM;

    public static void register(RegisterEvent event) {
        event.register(BuiltInRegistries.MENU.key(), helper -> {
            PHOTO_ALBUM = new MenuType<>(PhotoAlbumMenu::createClientMenu, FeatureFlags.VANILLA_SET);
            helper.register(RealmFinder.id("photo_album"), PHOTO_ALBUM);
        });
    }
}
