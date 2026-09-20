package com.hyisnoob.realmfinder.client.render;

import com.hyisnoob.realmfinder.common.item.PhotoTooltipData;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;

public class ClientPhotoTooltip implements ClientTooltipComponent {
    private final PhotoTooltipData data;

    public ClientPhotoTooltip(PhotoTooltipData data) {
        this.data = data;
    }

    @Override
    public int getHeight() {
        return 74;
    }

    @Override
    public int getWidth(Font font) {
        return 64;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        int cardW = 64;
        int cardH = 72;
        int photoSize = 56;
        int px = x + 4;
        int py = y + 4;

        // Shadow & paper
        guiGraphics.fill(x - 1, y - 1, x + cardW + 1, y + cardH + 1, 0x44000000);
        guiGraphics.fill(x, y, x + cardW, y + cardH, 0xFFFAF8F5);
        guiGraphics.fill(px - 1, py - 1, px + photoSize + 1, py + photoSize + 1, 0xFF2B2A28);

        // Photo
        ResourceLocation texture = PhotoCaptureHelper.getTexture(data.snapshotId());
        RenderSystem.enableBlend();
        guiGraphics.blit(texture, px, py, 0, 0, photoSize, photoSize, photoSize, photoSize);

        // Text
        String text = data.blockCount() + " blk";
        int textW = font.width(text);
        guiGraphics.drawString(font, text, x + (cardW - textW) / 2, y + photoSize + 6, 0xFF666666, false);
    }
}
