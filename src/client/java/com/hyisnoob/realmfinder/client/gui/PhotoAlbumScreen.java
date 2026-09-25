package com.hyisnoob.realmfinder.client.gui;

import com.hyisnoob.realmfinder.client.render.PhotoCaptureHelper;
import com.hyisnoob.realmfinder.common.item.ModItems;
import com.hyisnoob.realmfinder.common.menu.PhotoAlbumMenu;
import com.hyisnoob.realmfinder.core.service.PlatformHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class PhotoAlbumScreen extends AbstractContainerScreen<PhotoAlbumMenu> {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy", Locale.ROOT);
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.ROOT);
    private ItemStack lastHoveredPhoto = ItemStack.EMPTY;

    public PhotoAlbumScreen(PhotoAlbumMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 284;
        this.imageHeight = 176;
        this.inventoryLabelY = 82;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // A quiet archival spread: dark cloth cover, pale paper and one brass accent.
        guiGraphics.fill(x - 3, y - 3, x + imageWidth + 3, y + imageHeight + 3, 0xFF171B1B);
        guiGraphics.fill(x - 1, y - 1, x + imageWidth + 1, y + imageHeight + 1, 0xFF665940);
        guiGraphics.fill(x + 1, y + 1, x + 174, y + imageHeight - 1, 0xFFE5DEC9);
        guiGraphics.fill(x + 174, y + 1, x + 179, y + imageHeight - 1, 0xFF8C8068);
        guiGraphics.fill(x + 179, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFF0EAD9);
        guiGraphics.fill(x + 8, y + 26, x + 166, y + 27, 0xFFBDB19A);
        guiGraphics.fill(x + 8, y + 77, x + 166, y + 78, 0xFFBDB19A);
        guiGraphics.fill(x + 184, y + 24, x + 276, y + 25, 0xFFBDB19A);

        for (int slotIndex = 0; slotIndex < this.menu.slots.size(); slotIndex++) {
            Slot slot = this.menu.slots.get(slotIndex);
            int sx = x + slot.x;
            int sy = y + slot.y;
            if (slotIndex < PhotoAlbumMenu.ALBUM_SLOTS) {
                guiGraphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFFB7AB93);
                guiGraphics.fill(sx, sy, sx + 16, sy + 16, 0xFF776F60);
            } else {
                guiGraphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFFC7BCA7);
                guiGraphics.fill(sx, sy, sx + 16, sy + 16, 0xFF918B7D);
            }
        }

        // 5. Check currently hovered or last inspected photo for right page preview
        ItemStack currentPhoto = ItemStack.EMPTY;
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem() && this.hoveredSlot.getItem().is(ModItems.PHOTOGRAPH)) {
            currentPhoto = this.hoveredSlot.getItem();
            this.lastHoveredPhoto = currentPhoto.copy();
        } else if (!this.lastHoveredPhoto.isEmpty()) {
            currentPhoto = this.lastHoveredPhoto;
        }

        int rx = x + 186;
        int ry = y + 31;
        int previewSize = 84;
        guiGraphics.fill(rx - 3, ry - 3, rx + previewSize + 3, ry + previewSize + 3, 0xFFB8AA8D);
        guiGraphics.fill(rx - 2, ry - 2, rx + previewSize + 2, ry + previewSize + 2, 0xFFFFFFFF);
        guiGraphics.fill(rx, ry, rx + previewSize, ry + previewSize, 0xFF242A28);

        if (!currentPhoto.isEmpty()) {
            CompoundTag tag = PlatformHelper.getCustomTag(currentPhoto);
            if (tag.hasUUID("SnapshotId")) {
                UUID snapshotId = tag.getUUID("SnapshotId");
                int blockCount = tag.getInt("BlockCount");
                int entityCount = tag.getInt("EntityCount");
                long timestamp = tag.getLong("Timestamp");

                // Render Photo Texture
                ResourceLocation texture = PhotoCaptureHelper.getTexture(snapshotId, tag);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                guiGraphics.blit(texture, rx, ry, 0, 0, previewSize, previewSize, previewSize, previewSize);

                // Information below preview
                int infoY = y + 126;
                String dateStr = timestamp > 0 ? DATE_FORMAT.format(new Date(timestamp)) : "--";
                guiGraphics.drawString(this.font, dateStr, rx, infoY, 0xFF343A35, false);
                if (timestamp > 0) guiGraphics.drawString(this.font, TIME_FORMAT.format(new Date(timestamp)), rx, infoY + 11, 0xFF6A6F68, false);
                guiGraphics.drawString(this.font, Component.translatable("screen.realmfinder.blocks", blockCount), rx, infoY + 24, 0xFF343A35, false);
                guiGraphics.drawString(this.font, Component.translatable("screen.realmfinder.entities", entityCount), rx, infoY + 35, 0xFF343A35, false);
            }
        } else {
            guiGraphics.drawString(this.font, Component.translatable("screen.realmfinder.select_photo"), rx + 9, ry + 37, 0xFF8F968D, false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, Component.translatable("screen.realmfinder.archive"), 8, 10, 0xFF344642, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.realmfinder.slots"), 120, 10, 0xFF806F4E, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xFF565C53, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.realmfinder.preview"), 184, 10, 0xFF344642, false);
    }
}
