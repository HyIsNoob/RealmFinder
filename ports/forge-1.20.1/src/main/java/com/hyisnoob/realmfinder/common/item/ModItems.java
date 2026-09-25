package com.hyisnoob.realmfinder.common.item;

import com.hyisnoob.realmfinder.RealmFinder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegisterEvent;

public class ModItems {

    public static Item CAMERA;
    public static Item EMPTY_PHOTOGRAPH;
    public static Item PHOTOGRAPH;
    public static Item PHOTO_ALBUM;

    public static void register(RegisterEvent event) {
        event.register(BuiltInRegistries.ITEM.key(), helper -> {
            CAMERA = new CameraItem(new Item.Properties().durability(100));
            EMPTY_PHOTOGRAPH = new Item(new Item.Properties().stacksTo(64));
            PHOTOGRAPH = new PhotographItem(new Item.Properties().stacksTo(16));
            PHOTO_ALBUM = new PhotoAlbumItem(new Item.Properties().stacksTo(1));
            helper.register(RealmFinder.id("camera"), CAMERA);
            helper.register(RealmFinder.id("empty_photograph"), EMPTY_PHOTOGRAPH);
            helper.register(RealmFinder.id("photograph"), PHOTOGRAPH);
            helper.register(RealmFinder.id("photo_album"), PHOTO_ALBUM);
        });
    }
}
