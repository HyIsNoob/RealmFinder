package com.hyisnoob.realmfinder.common.item;

import com.hyisnoob.realmfinder.RealmFinder;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public class ModItems {

    public static final Item CAMERA = new CameraItem(new Item.Properties().stacksTo(1));
    public static final Item PHOTOGRAPH = new PhotographItem(new Item.Properties().stacksTo(16));
    public static final Item PHOTO_ALBUM = new PhotoAlbumItem(new Item.Properties().stacksTo(1));

    public static void register() {
        Registry.register(BuiltInRegistries.ITEM, RealmFinder.id("camera"), CAMERA);
        Registry.register(BuiltInRegistries.ITEM, RealmFinder.id("photograph"), PHOTOGRAPH);
        Registry.register(BuiltInRegistries.ITEM, RealmFinder.id("photo_album"), PHOTO_ALBUM);

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(content -> {
            content.accept(CAMERA);
            content.accept(PHOTOGRAPH);
            content.accept(PHOTO_ALBUM);
        });
    }
}
