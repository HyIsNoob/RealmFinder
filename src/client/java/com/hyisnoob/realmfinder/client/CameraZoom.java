package com.hyisnoob.realmfinder.client;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;

public final class CameraZoom {
    private static int zoom = 1;

    private CameraZoom() {}

    public static int get() {
        return Math.min(zoom, ClientPreferences.get().maxCameraZoom);
    }

    public static void cycle(int direction) {
        int next = Math.clamp(zoom + direction, 1, ClientPreferences.get().maxCameraZoom);
        if (next != zoom) {
            zoom = next;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) mc.player.playSound(SoundEvents.SPYGLASS_USE, 0.5f, 0.8f + zoom * 0.1f);
        }
    }
}
