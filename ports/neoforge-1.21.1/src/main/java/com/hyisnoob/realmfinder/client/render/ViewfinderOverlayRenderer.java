package com.hyisnoob.realmfinder.client.render;

import com.hyisnoob.realmfinder.common.item.ModItems;
import com.hyisnoob.realmfinder.core.service.PlatformHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.UUID;

public class ViewfinderOverlayRenderer {

    private static float animProgress = 0.0f;
    private static long stampAnimationStart = 0;
    private static final long STAMP_ANIM_DURATION = 300;

    private static final float[] SCALES = {0.5f, 1.0f, 2.0f, 3.0f};
    private static final String[] SCALE_NAMES = {"0.5x [FAR / ADD]", "1.0x [NORMAL]", "2.0x [CLOSE]", "3.0x [NEAR]"};
    private static int currentScaleIndex = 1; // Default 1.0x

    public static float getCurrentScale() {
        return SCALES[currentScaleIndex];
    }

    public static String getScaleName() {
        return SCALE_NAMES[currentScaleIndex];
    }

    public static void cycleScale(int direction) {
        int newIndex = currentScaleIndex + direction;
        if (newIndex >= 0 && newIndex < SCALES.length) {
            currentScaleIndex = newIndex;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.playSound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME.value(), 0.8f, 1.0f + currentScaleIndex * 0.25f);
            }
        }
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((RenderGuiEvent.Post event) ->
                renderOverlay(event.getGuiGraphics(), event.getPartialTick()));
    }

    public static void triggerStampAnimation() {
        stampAnimationStart = System.currentTimeMillis();
    }

    private static void renderOverlay(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        // Only in first-person mode
        if (!mc.options.getCameraType().isFirstPerson()) {
            animProgress = 0.0f;
            return;
        }

        ItemStack main = mc.player.getMainHandItem();
        ItemStack off = mc.player.getOffhandItem();
        ItemStack photoStack = main.is(ModItems.CAMERA) || off.is(ModItems.CAMERA) ? null
                : main.is(ModItems.PHOTOGRAPH) ? main : (off.is(ModItems.PHOTOGRAPH) ? off : null);

        boolean holdingPhoto = photoStack != null;

        // Smooth ease-in / ease-out animation
        float target = holdingPhoto ? 1.0f : 0.0f;
        float speed = deltaTracker.getGameTimeDeltaPartialTick(false) * 0.25f;
        if (animProgress < target) {
            animProgress = Math.min(target, animProgress + speed);
        } else if (animProgress > target) {
            animProgress = Math.max(target, animProgress - speed);
        }

        if (animProgress <= 0.001f || photoStack == null) {
            return;
        }

        CompoundTag tag = PlatformHelper.getCustomTag(photoStack);
        if (!tag.hasUUID("SnapshotId")) {
            return;
        }

        UUID snapshotId = tag.getUUID("SnapshotId");
        int blockCount = tag.getInt("BlockCount");
        int entityCount = tag.getInt("EntityCount");

        int screenW = guiGraphics.guiWidth();
        int screenH = guiGraphics.guiHeight();

        // The paper frame gives a small visual cue while the world outline shows exact placement.
        float focusDistance = Math.max(4.0f, tag.getFloat("FarPlane") - 6.0f);
        float farVisualScale = Math.clamp(focusDistance / (focusDistance + 12.0f), 0.35f, 0.80f);
        float visualScaleMult = currentScaleIndex == 0 ? farVisualScale
                : (currentScaleIndex == 2 ? 1.12f : (currentScaleIndex == 3 ? 1.20f : 1.0f));
        int baseSize = (int) (screenH * 0.52f * visualScaleMult);
        baseSize = (baseSize / 2) * 2;

        // Angle Helper Calculations
        float yaw = mc.player.getYRot();
        float nearestYaw = Math.round(yaw / 90.0f) * 90.0f;
        float deltaYaw = (yaw - nearestYaw) % 360.0f;
        if (deltaYaw > 180.0f) deltaYaw -= 360.0f;
        if (deltaYaw < -180.0f) deltaYaw += 360.0f;

        float pitch = mc.player.getXRot();
        float nearestPitch = Math.abs(pitch) < 22.5f ? 0.0f : Math.round(pitch / 45.0f) * 45.0f;
        float deltaPitch = pitch - nearestPitch;

        boolean isSneaking = mc.player.isShiftKeyDown();

        // Magnetic Snap Assist when holding Shift
        if (isSneaking) {
            if (Math.abs(deltaYaw) < 14.0f) {
                mc.player.setYRot(mc.player.getYRot() - deltaYaw * 0.22f);
            }
            if (Math.abs(deltaPitch) < 14.0f) {
                mc.player.setXRot(mc.player.getXRot() - deltaPitch * 0.22f);
            }
        }

        boolean isAligned = Math.abs(deltaYaw) < 6.0f && Math.abs(deltaPitch) < 6.0f;

        int cardinal = ((int) Math.floor((nearestYaw + 45.0f) / 90.0f) % 4 + 4) % 4;
        String[] dirNames = {"SOUTH", "WEST", "NORTH", "EAST"};
        String currentDir = dirNames[cardinal];

        // Stamp dissolve animation
        long now = System.currentTimeMillis();
        long stampElapsed = now - stampAnimationStart;
        boolean inStampAnim = stampElapsed >= 0 && stampElapsed < STAMP_ANIM_DURATION;
        float stampScale = 1.0f;
        float stampFade = 1.0f;
        if (inStampAnim) {
            float p = (float) stampElapsed / STAMP_ANIM_DURATION;
            stampScale = 1.0f + p * 0.15f;
            stampFade = 1.0f - p;
        }

        // Cubic ease-out
        float t = animProgress;
        float ease = 1.0f - (float) Math.pow(1.0f - t, 3);

        int photoSize = (int) (baseSize * stampScale);
        int slideY = (int) ((1.0f - ease) * 40.0f);

        int photoX = (screenW - photoSize) / 2;
        int photoY = (screenH - photoSize) / 2 + slideY;

        int border = 4;
        int frameX = photoX - border;
        int frameY = photoY - border;
        int frameW = photoSize + border * 2;
        int frameH = photoSize + border * 2;

        float alpha = (isSneaking ? 0.65f : 1.0f) * stampFade;

        // 1. Soft Drop Shadow
        guiGraphics.fill(frameX - 2, frameY - 2, frameX + frameW + 2, frameY + frameH + 2, 0x55000000);

        // 2. Paper Border Color:
        // Glowing Emerald Green if Aligned, Amber/Orange if Shift, Ivory if default
        int paperColor;
        if (!FrustumGhostRenderer.isValid()) {
            paperColor = 0xFFFF5555;
        } else if (isAligned) {
            paperColor = 0xFF00FF88; // Emerald Green glow
        } else if (isSneaking) {
            paperColor = 0xFFFFAA00; // Orange align mode
        } else {
            paperColor = 0xFFFAF8F5; // Clean ivory white paper
        }
        guiGraphics.fill(frameX, frameY, frameX + frameW, frameY + frameH, paperColor);

        // 3. Render Captured Photo
        ResourceLocation texture = PhotoCaptureHelper.getTexture(snapshotId, tag);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
        guiGraphics.blit(texture, photoX, photoY, 0, 0, photoSize, photoSize, photoSize, photoSize);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        // 4. Clean HUD Panels (Top Badge + Left Wing + Right Wing, zero overlap with hotbar)
        if (ease > 0.8f && !inStampAnim) {
            // A. Top Alignment Status Badge (Centered above the photo)
            int topY = frameY - 15;
            if (!FrustumGhostRenderer.isValid()) {
                String badge = "TARGET BLOCKED";
                int bw = mc.font.width(badge);
                int bx = (screenW - bw) / 2;
                guiGraphics.fill(bx - 6, topY - 2, bx + bw + 6, topY + 10, 0xCC401010);
                guiGraphics.drawString(mc.font, badge, bx, topY, 0xFFFF5555, true);
            } else if (isAligned) {
                String badge = "LOCKED: " + currentDir + "  |  LEVEL 0°";
                int bw = mc.font.width(badge);
                int bx = (screenW - bw) / 2;
                guiGraphics.fill(bx - 6, topY - 2, bx + bw + 6, topY + 10, 0xCC002B16);
                guiGraphics.drawString(mc.font, badge, bx, topY, 0xFF00FF88, true);
            } else {
                int roundPitch = Math.round(pitch);
                String pitchStr = (roundPitch > 0 ? "+" : (roundPitch < 0 ? "-" : "")) + Math.abs(roundPitch) + "°";
                String guide = "DIR: " + currentDir + "  |  PITCH: " + pitchStr + "  (HOLD SHIFT TO SNAP)";
                int gw = mc.font.width(guide);
                int gx = (screenW - gw) / 2;
                guiGraphics.fill(gx - 6, topY - 2, gx + gw + 6, topY + 10, 0x99000000);
                guiGraphics.drawString(mc.font, guide, gx, topY, 0xFFAAAAAA, true);
            }

            // B. Left Wing Panel: Snapshot Content (Blocks, Entities, Grid Status)
            int leftCardW = 104;
            int leftCardH = 48;
            int leftCardX = Math.max(6, frameX - leftCardW - 10);
            int leftCardY = frameY + 8;

            // Draw Left Card Background & 1px Outline
            guiGraphics.fill(leftCardX - 1, leftCardY - 1, leftCardX + leftCardW + 1, leftCardY + leftCardH + 1, 0x55000000);
            guiGraphics.fill(leftCardX, leftCardY, leftCardX + leftCardW, leftCardY + leftCardH, 0xD00A111A);
            int leftBorderCol = isAligned ? 0x8800FF88 : 0x5500E5FF;
            drawCardOutline(guiGraphics, leftCardX, leftCardY, leftCardW, leftCardH, leftBorderCol);

            // Left Card Content
            guiGraphics.drawString(mc.font, "SNAPSHOT", leftCardX + 6, leftCardY + 4, 0xFF00E5FF, true);
            guiGraphics.fill(leftCardX + 5, leftCardY + 14, leftCardX + leftCardW - 5, leftCardY + 15, 0x33FFFFFF);
            guiGraphics.drawString(mc.font, "Blocks: " + String.format(Locale.ROOT, "%,d", blockCount), leftCardX + 6, leftCardY + 18, 0xFFE0E0E0, true);
            guiGraphics.drawString(mc.font, "Entities: " + entityCount, leftCardX + 6, leftCardY + 28, 0xFFE0E0E0, true);
            String gridTag = FrustumGhostRenderer.getStatusText();
            int gridCol = FrustumGhostRenderer.isValid() ? 0xFF00FF88 : 0xFFFF5555;
            guiGraphics.drawString(mc.font, gridTag, leftCardX + 6, leftCardY + 38, gridCol, true);

            // C. Right Wing Panel: Perspective Scale & Controls
            int rightCardW = 116;
            int rightCardH = 58;
            int rightCardX = Math.min(screenW - rightCardW - 6, frameX + frameW + 10);
            int rightCardY = frameY + 8;

            // Draw Right Card Background & 1px Outline
            guiGraphics.fill(rightCardX - 1, rightCardY - 1, rightCardX + rightCardW + 1, rightCardY + rightCardH + 1, 0x55000000);
            guiGraphics.fill(rightCardX, rightCardY, rightCardX + rightCardW, rightCardY + rightCardH, 0xD00A111A);
            drawCardOutline(guiGraphics, rightCardX, rightCardY, rightCardW, rightCardH, 0x55FFCC00);

            // Right Card Content
            guiGraphics.drawString(mc.font, "PERSPECTIVE", rightCardX + 6, rightCardY + 4, 0xFFFFCC00, true);
            guiGraphics.fill(rightCardX + 5, rightCardY + 14, rightCardX + rightCardW - 5, rightCardY + 15, 0x33FFFFFF);
            int sCol = currentScaleIndex == 1 ? 0xFFFFFFFF : (currentScaleIndex == 0 ? 0xFF88CCFF : 0xFFFFCC00);
            guiGraphics.drawString(mc.font, "Zoom: " + getScaleName(), rightCardX + 6, rightCardY + 18, sCol, true);
            if (com.hyisnoob.realmfinder.client.ClientPreferences.get().showHudHints)
                guiGraphics.drawString(mc.font, "Scroll / [ ]", rightCardX + 6, rightCardY + 28, 0xFF888888, true);
            String placeAction = isSneaking ? "R-Click: Additive" : "R-Click: Place";
            int placeCol = isSneaking ? 0xFFFFAA00 : 0xFF88FF88;
            guiGraphics.drawString(mc.font, placeAction, rightCardX + 6, rightCardY + 38, placeCol, true);
            if (com.hyisnoob.realmfinder.client.ClientPreferences.get().showHudHints)
                guiGraphics.drawString(mc.font, "Shift: Snap / Add", rightCardX + 6, rightCardY + 48, 0xFFAAAAAA, true);

            // ZERO text below the photo! Entire hotbar, hearts, and hunger bar remain 100% unobstructed!
        }
    }

    private static void drawCardOutline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }
}
