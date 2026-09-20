package com.hyisnoob.realmfinder.common.menu;

import com.hyisnoob.realmfinder.RealmFinder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public class ModMenus {

    public static final MenuType<PhotoAlbumMenu> PHOTO_ALBUM = new MenuType<>(PhotoAlbumMenu::createClientMenu, FeatureFlags.VANILLA_SET);

    public static void register() {
        Registry.register(BuiltInRegistries.MENU, RealmFinder.id("photo_album"), PHOTO_ALBUM);
    }
}
