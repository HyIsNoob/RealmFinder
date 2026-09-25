package com.hyisnoob.realmfinder.common.item;

import com.hyisnoob.realmfinder.common.menu.PhotoAlbumMenu;
import com.hyisnoob.realmfinder.core.service.PlatformHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class PhotoAlbumItem extends Item {

    public PhotoAlbumItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack albumStack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            SimpleContainer container = new SimpleContainer(PhotoAlbumMenu.ALBUM_SLOTS);
            loadInventory(albumStack, container);

            serverPlayer.openMenu(new SimpleMenuProvider(
                    (syncId, inv, p) -> new PhotoAlbumMenu(syncId, inv, container, albumStack, hand),
                    Component.translatable("item.realmfinder.photo_album")
            ));

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8f, 1.0f);
        }
        return InteractionResultHolder.sidedSuccess(albumStack, level.isClientSide);
    }

    public static void loadInventory(ItemStack stack, Container container) {
        CompoundTag tag = PlatformHelper.getCustomTag(stack);
        if (tag.contains("AlbumItems", 10)) {
            CompoundTag itemsTag = tag.getCompound("AlbumItems");
            NonNullList<ItemStack> list = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
            ContainerHelper.loadAllItems(itemsTag, list);
            for (int i = 0; i < list.size(); i++) {
                container.setItem(i, list.get(i));
            }
        }
    }

    public static void saveInventory(ItemStack stack, Container container) {
        CompoundTag tag = PlatformHelper.getCustomTag(stack);
        CompoundTag itemsTag = new CompoundTag();
        NonNullList<ItemStack> list = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < container.getContainerSize(); i++) {
            list.set(i, container.getItem(i));
        }
        ContainerHelper.saveAllItems(itemsTag, list);
        tag.put("AlbumItems", itemsTag);
        PlatformHelper.setCustomTag(stack, tag);
    }

}
