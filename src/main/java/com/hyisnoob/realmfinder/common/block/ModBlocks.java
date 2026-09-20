package com.hyisnoob.realmfinder.common.block;

import com.hyisnoob.realmfinder.RealmFinder;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModBlocks {

    public static final Block PHOTO_STAND = new PhotoStandBlock(
            BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
    );

    public static final Item PHOTO_STAND_ITEM = new BlockItem(
            PHOTO_STAND,
            new Item.Properties().stacksTo(16)
    );

    public static final BlockEntityType<PhotoStandBlockEntity> PHOTO_STAND_BE =
            FabricBlockEntityTypeBuilder.create(PhotoStandBlockEntity::new, PHOTO_STAND).build();

    public static void register() {
        Registry.register(BuiltInRegistries.BLOCK, RealmFinder.id("photo_stand"), PHOTO_STAND);
        Registry.register(BuiltInRegistries.ITEM, RealmFinder.id("photo_stand"), PHOTO_STAND_ITEM);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, RealmFinder.id("photo_stand"), PHOTO_STAND_BE);

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(content -> {
            content.accept(PHOTO_STAND_ITEM);
        });
    }
}
