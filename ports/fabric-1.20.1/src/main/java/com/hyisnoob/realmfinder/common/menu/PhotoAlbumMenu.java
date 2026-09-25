package com.hyisnoob.realmfinder.common.menu;

import com.hyisnoob.realmfinder.common.item.ModItems;
import com.hyisnoob.realmfinder.common.item.PhotoAlbumItem;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PhotoAlbumMenu extends AbstractContainerMenu {

    public static final int ALBUM_SLOTS = 18;
    private final Container albumContainer;
    private final ItemStack albumStack;
    private final InteractionHand heldHand;

    public static PhotoAlbumMenu createClientMenu(int syncId, Inventory playerInventory) {
        return new PhotoAlbumMenu(syncId, playerInventory, new SimpleContainer(ALBUM_SLOTS), ItemStack.EMPTY,
                InteractionHand.MAIN_HAND);
    }

    public PhotoAlbumMenu(int syncId, Inventory playerInventory, Container albumContainer, ItemStack albumStack,
                          InteractionHand heldHand) {
        super(ModMenus.PHOTO_ALBUM, syncId);
        this.albumContainer = albumContainer;
        this.albumStack = albumStack;
        this.heldHand = heldHand;
        checkContainerSize(albumContainer, ALBUM_SLOTS);
        albumContainer.startOpen(playerInventory.player);

        // Album page: two rows of nine photographs.
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9;
                this.addSlot(new Slot(albumContainer, index, 8 + col * 18, 34 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.is(ModItems.PHOTOGRAPH);
                    }

                    @Override
                    public void setChanged() {
                        super.setChanged();
                        if (!PhotoAlbumMenu.this.albumStack.isEmpty()
                                && !playerInventory.player.level().isClientSide) {
                            PhotoAlbumItem.saveInventory(PhotoAlbumMenu.this.albumStack,
                                    PhotoAlbumMenu.this.albumContainer);
                        }
                    }
                });
            }
        }

        // Player inventory below the archive page.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 94 + row * 18));
            }
        }

        // Hotbar.
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 154));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return albumStack.isEmpty() || (player.getItemInHand(heldHand) == albumStack
                && albumContainer.stillValid(player));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.albumContainer.stopOpen(player);
        if (!player.level().isClientSide && !albumStack.isEmpty()) {
            PhotoAlbumItem.saveInventory(albumStack, albumContainer);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            if (index < ALBUM_SLOTS) {
                // Move from album to player inventory
                if (!this.moveItemStackTo(stackInSlot, ALBUM_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from player inventory to album (only photographs)
                if (stackInSlot.is(ModItems.PHOTOGRAPH)) {
                    if (!this.moveItemStackTo(stackInSlot, 0, ALBUM_SLOTS, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }
}
