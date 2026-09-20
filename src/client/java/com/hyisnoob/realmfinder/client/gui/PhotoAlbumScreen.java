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

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT);
    private ItemStack lastHoveredPhoto = ItemStack.EMPTY;

    public PhotoAlbumScreen(PhotoAlbumMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 284;
        this.imageHeight = 152;
        this.inventoryLabelY = 56;
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

        // 1. Book Outer Cover / Border (Deep Vintage Leather #1A130E)
        guiGraphics.fill(x - 3, y - 3, x + this.imageWidth + 3, y + this.imageHeight + 3, 0xFF140E0A);
        guiGraphics.fill(x - 2, y - 2, x + this.imageWidth + 2, y + this.imageHeight + 2, 0xFF2A1E14);
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF3D2D1E);

        // 2. Left Page (Slots area, 172 px wide, warm parchment #D8CAB6)
        guiGraphics.fill(x + 2, y + 2, x + 172, y + this.imageHeight - 2, 0xFFC8B9A6);
        // Spine divider
        guiGraphics.fill(x + 172, y + 2, x + 176, y + this.imageHeight - 2, 0xFF22170E);

        // 3. Right Page (Gallery preview area, 106 px wide, parchment #E0D4C3)
        guiGraphics.fill(x + 176, y + 2, x + this.imageWidth - 2, y + this.imageHeight - 2, 0xFFD0C2AF);

        // 4. Draw slot backgrounds on the left page
        for (Slot slot : this.menu.slots) {
            int sx = x + slot.x;
            int sy = y + slot.y;
            if (slot.index < PhotoAlbumMenu.ALBUM_SLOTS) {
                // Album slot: subtle golden inset
                guiGraphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF9E8569);
                guiGraphics.fill(sx, sy, sx + 16, sy + 16, 0xFF5C4B38);
            } else {
                // Player inventory slots: standard inset
                guiGraphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFFA09280);
                guiGraphics.fill(sx, sy, sx + 16, sy + 16, 0xFF8A7C6B);
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

        // 6. Render Right Page (Gallery Preview)
        int rx = x + 180;
        int ry = y + 8;
        int previewSize = 92;

        // Photo Frame Border (Polaroid White)
        guiGraphics.fill(rx - 2, ry - 2, rx + previewSize + 2, ry + previewSize + 2, 0xFFFFFFFF);
        guiGraphics.fill(rx, ry, rx + previewSize, ry + previewSize, 0xFF181818);

        if (!currentPhoto.isEmpty()) {
            CompoundTag tag = PlatformHelper.getCustomTag(currentPhoto);
            if (tag.hasUUID("SnapshotId")) {
                UUID snapshotId = tag.getUUID("SnapshotId");
                int blockCount = tag.getInt("BlockCount");
                int entityCount = tag.getInt("EntityCount");
                long timestamp = tag.getLong("Timestamp");

                // Render Photo Texture
                ResourceLocation texture = PhotoCaptureHelper.getTexture(snapshotId);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                guiGraphics.blit(texture, rx, ry, 0, 0, previewSize, previewSize, previewSize, previewSize);

                // Information below preview
                int infoY = ry + previewSize + 6;
                String dateStr = timestamp > 0 ? DATE_FORMAT.format(new Date(timestamp)) : "Recent Snapshot";
                guiGraphics.drawString(this.font, dateStr, rx, infoY, 0xFF3D2D1E, false);
                guiGraphics.drawString(this.font, "Blocks: " + String.format(Locale.ROOT, "%,d", blockCount), rx, infoY + 11, 0xFF2A1E14, false);
                if (entityCount > 0) {
                    guiGraphics.drawString(this.font, "Entities: " + entityCount, rx, infoY + 22, 0xFF145228, false);
                }
            }
        } else {
            // Empty placeholder text
            guiGraphics.drawString(this.font, "SELECT PHOTO", rx + 14, ry + 36, 0xFF666666, false);
            guiGraphics.drawString(this.font, "TO INSPECT", rx + 20, ry + 48, 0xFF888888, false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, "PHOTO ALBUM (18 SLOTS)", 8, 6, 0xFF2A1E14, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xFF3D2D1E, false);
    }
}
