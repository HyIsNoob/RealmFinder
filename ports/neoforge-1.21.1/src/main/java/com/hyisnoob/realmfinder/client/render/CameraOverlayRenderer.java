package com.hyisnoob.realmfinder.client.render;

import com.hyisnoob.realmfinder.client.CameraZoom;
import com.hyisnoob.realmfinder.common.item.ModItems;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public class CameraOverlayRenderer {

    private static long shutterTriggerTime = 0;
    private static final long SHUTTER_DURATION_MS = 200;

    public static void register() {
        NeoForge.EVENT_BUS.addListener((RenderGuiEvent.Post event) ->
                renderOverlay(event.getGuiGraphics(), event.getPartialTick()));
    }

    public static void triggerShutter() {
        shutterTriggerTime = System.currentTimeMillis();
    }

    private static void renderOverlay(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        // Check if camera is held in first-person
        ItemStack main = mc.player.getMainHandItem();
        ItemStack off = mc.player.getOffhandItem();
        boolean holdingCamera = main.is(ModItems.CAMERA) || off.is(ModItems.CAMERA);

        long now = System.currentTimeMillis();
        long elapsed = now - shutterTriggerTime;
        boolean inShutter = elapsed >= 0 && elapsed < SHUTTER_DURATION_MS;

        // Render Shutter Flash if triggered
        if (inShutter) {
            float progress = (float) elapsed / SHUTTER_DURATION_MS;
            float alpha = 1.0f - progress;
            int alphaInt = (int) (alpha * 240);
            if (alphaInt > 0) {
                int color = (alphaInt << 24) | 0x00FFFFFF;
                guiGraphics.fill(0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(), color);
            }
        }

        // Suppress HUD overlay during capture frame so the photo doesn't capture UI elements
        if (PhotoCaptureHelper.isPendingCapture()) {
            return;
        }

        // Only render viewfinder HUD if holding camera and in first-person view
        if (!holdingCamera || !mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        int screenW = guiGraphics.guiWidth();
        int screenH = guiGraphics.guiHeight();

        // Viewfinder framing square (exact same size and position as the photo!)
        int frameSize = (int) (screenH * 0.52f);
        frameSize = (frameSize / 2) * 2; // Keep even

        int frameX = (screenW - frameSize) / 2;
        int frameY = (screenH - frameSize) / 2;

        // Angle & Leveling Calculations
        float yaw = mc.player.getYRot();
        float nearestYaw = Math.round(yaw / 90.0f) * 90.0f;
        float deltaYaw = (yaw - nearestYaw) % 360.0f;
        if (deltaYaw > 180.0f) deltaYaw -= 360.0f;
        if (deltaYaw < -180.0f) deltaYaw += 360.0f;

        float pitch = mc.player.getXRot();
        float deltaPitch = pitch; // Level is 0.0f

        boolean isSneaking = mc.player.isShiftKeyDown();

        // Magnetic Level & Snap Assist when holding Shift
        if (isSneaking) {
            if (Math.abs(deltaYaw) < 18.0f) {
                mc.player.setYRot(mc.player.getYRot() - deltaYaw * 0.22f);
            }
            if (Math.abs(deltaPitch) < 18.0f) {
                mc.player.setXRot(mc.player.getXRot() - deltaPitch * 0.22f);
            }
        }

        boolean isAligned = Math.abs(deltaYaw) < 6.0f && Math.abs(deltaPitch) < 6.0f;

        int cardinal = ((int) Math.floor((nearestYaw + 45.0f) / 90.0f) % 4 + 4) % 4;
        String[] dirNames = {"SOUTH", "WEST", "NORTH", "EAST"};
        String currentDir = dirNames[cardinal];

        // 1. Dark letterbox mask around camera view (like holding up a camera screen)
        int maskColor = 0x77000000;
        guiGraphics.fill(0, 0, screenW, frameY, maskColor);
        guiGraphics.fill(0, frameY + frameSize, screenW, screenH, maskColor);
        guiGraphics.fill(0, frameY, frameX, frameY + frameSize, maskColor);
        guiGraphics.fill(frameX + frameSize, frameY, screenW, frameY + frameSize, maskColor);

        // 2. Camera screen frame border (rounded dark metallic frame)
        int bezelColor = isAligned ? 0xFF1B3828 : 0xFF1C1B1A;
        int bezelWidth = 6;
        guiGraphics.fill(frameX - bezelWidth, frameY - bezelWidth, frameX + frameSize + bezelWidth, frameY, bezelColor);
        guiGraphics.fill(frameX - bezelWidth, frameY + frameSize, frameX + frameSize + bezelWidth, frameY + frameSize + bezelWidth, bezelColor);
        guiGraphics.fill(frameX - bezelWidth, frameY, frameX, frameY + frameSize, bezelColor);
        guiGraphics.fill(frameX + frameSize, frameY, frameX + frameSize + bezelWidth, frameY + frameSize, bezelColor);

        // Thin inner screen edge highlight
        int highlightColor = isAligned ? 0x6600FF88 : 0x44FFFFFF;
        guiGraphics.fill(frameX - 1, frameY - 1, frameX + frameSize + 1, frameY, highlightColor);
        guiGraphics.fill(frameX - 1, frameY + frameSize, frameX + frameSize + 1, frameY + frameSize + 1, 0x44000000);

        // 3. Subtle Rule-of-Thirds Composition Grid lines
        int gridColor = 0x18FFFFFF;
        int thirdW = frameSize / 3;
        int thirdH = frameSize / 3;
        guiGraphics.fill(frameX + thirdW, frameY, frameX + thirdW + 1, frameY + frameSize, gridColor);
        guiGraphics.fill(frameX + thirdW * 2, frameY, frameX + thirdW * 2 + 1, frameY + frameSize, gridColor);
        guiGraphics.fill(frameX, frameY + thirdH, frameX + frameSize, frameY + thirdH + 1, gridColor);
        guiGraphics.fill(frameX, frameY + thirdH * 2, frameX + frameSize, frameY + thirdH * 2 + 1, gridColor);

        // 4. Clean minimal focus brackets in the center (glows Emerald Green when aligned)
        int cx = frameX + frameSize / 2;
        int cy = frameY + frameSize / 2;
        int reticleSize = 16;
        int reticleThick = 2;
        int bColor = isAligned ? 0xFF00FF88 : 0xCCFFFFFF;

        // Top-Left bracket
        guiGraphics.fill(cx - reticleSize, cy - reticleSize, cx - reticleSize + 6, cy - reticleSize + reticleThick, bColor);
        guiGraphics.fill(cx - reticleSize, cy - reticleSize, cx - reticleSize + reticleThick, cy - reticleSize + 6, bColor);
        // Top-Right bracket
        guiGraphics.fill(cx + reticleSize - 6, cy - reticleSize, cx + reticleSize, cy - reticleSize + reticleThick, bColor);
        guiGraphics.fill(cx + reticleSize - reticleThick, cy - reticleSize, cx + reticleSize, cy - reticleSize + 6, bColor);
        // Bottom-Left bracket
        guiGraphics.fill(cx - reticleSize, cy + reticleSize - reticleThick, cx - reticleSize + 6, cy + reticleSize, bColor);
        guiGraphics.fill(cx - reticleSize, cy + reticleSize - 6, cx - reticleSize + reticleThick, cy + reticleSize, bColor);
        // Bottom-Right bracket
        guiGraphics.fill(cx + reticleSize - 6, cy + reticleSize - reticleThick, cx + reticleSize, cy + reticleSize, bColor);
        guiGraphics.fill(cx + reticleSize - reticleThick, cy + reticleSize - 6, cx + reticleSize, cy + reticleSize, bColor);

        // 5. Auto-Focus Rangefinder Meter (Top-Right of screen)
        net.minecraft.world.phys.HitResult hit = mc.player.pick(36.0, 0.0f, false);
        String afText;
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            double dist = hit.getLocation().distanceTo(mc.player.getEyePosition());
            afText = String.format(java.util.Locale.ROOT, "RANGE: %.1fm", dist);
        } else {
            afText = "RANGE: INF";
        }
        int afw = mc.font.width(afText);
        int afx = frameX + frameSize - afw - 8;
        int afy = frameY + 8;
        guiGraphics.fill(afx - 4, afy - 2, afx + afw + 4, afy + 10, 0x88000000);
        guiGraphics.drawString(mc.font, afText, afx, afy, 0xFF00E5FF, true);

        // 6. Angle Alignment Status Badge (Rendered cleanly above the viewfinder frame)
        if (isAligned) {
            String badge = "LOCKED: " + currentDir + "  |  LEVEL 0°";
            int bw = mc.font.width(badge);
            int bx = (screenW - bw) / 2;
            int by = frameY - 14;
            guiGraphics.fill(bx - 6, by - 2, bx + bw + 6, by + 10, 0xCC002B16);
            guiGraphics.drawString(mc.font, badge, bx, by, 0xFF00FF88, true);
        } else {
            int roundPitch = Math.round(pitch);
            String pitchStr = (roundPitch > 0 ? "+" : (roundPitch < 0 ? "-" : "")) + Math.abs(roundPitch) + "°";
            String guide = "DIR: " + currentDir + "  |  PITCH: " + pitchStr + "  (HOLD SHIFT TO LEVEL)";
            int gw = mc.font.width(guide);
            int gx = (screenW - gw) / 2;
            int gy = frameY - 14;
            guiGraphics.fill(gx - 4, gy - 2, gx + gw + 4, gy + 10, 0x99000000);
            guiGraphics.drawString(mc.font, guide, gx, gy, 0xFFAAAAAA, true);
        }

        // 7. Clean instruction footer
        if (com.hyisnoob.realmfinder.client.ClientPreferences.get().showHudHints) {
            String hint = "[R-CLICK] SNAP   [CTRL+SCROLL] ZOOM " + CameraZoom.get() + "x   [SHIFT] LEVEL";
            int hw = mc.font.width(hint);
            guiGraphics.drawString(mc.font, hint, (screenW - hw) / 2, frameY + frameSize + bezelWidth + 8, 0xFFCCCCCC, true);
        }
    }
}
