package com.hyisnoob.realmfinder.common.block;

import com.hyisnoob.realmfinder.RealmFinder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.RegisterEvent;

public class ModBlocks {

    public static Block PHOTO_STAND;
    public static Item PHOTO_STAND_ITEM;
    public static BlockEntityType<PhotoStandBlockEntity> PHOTO_STAND_BE;

    public static void register(RegisterEvent event) {
        event.register(BuiltInRegistries.BLOCK.key(), helper -> {
            PHOTO_STAND = new PhotoStandBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f).sound(SoundType.WOOD).noOcclusion());
            helper.register(RealmFinder.id("photo_stand"), PHOTO_STAND);
        });
        event.register(BuiltInRegistries.ITEM.key(), helper -> {
            PHOTO_STAND_ITEM = new BlockItem(PHOTO_STAND, new Item.Properties().stacksTo(16));
            helper.register(RealmFinder.id("photo_stand"), PHOTO_STAND_ITEM);
        });
        event.register(BuiltInRegistries.BLOCK_ENTITY_TYPE.key(), helper -> {
            PHOTO_STAND_BE = BlockEntityType.Builder.of(PhotoStandBlockEntity::new, PHOTO_STAND).build(null);
            helper.register(RealmFinder.id("photo_stand"), PHOTO_STAND_BE);
        });
    }
}
