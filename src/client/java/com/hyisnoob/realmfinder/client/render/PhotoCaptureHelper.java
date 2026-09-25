package com.hyisnoob.realmfinder.client.render;

import com.hyisnoob.realmfinder.RealmFinder;
import com.hyisnoob.realmfinder.client.CameraZoom;
import com.hyisnoob.realmfinder.common.network.TakePhotoPayload;
import com.hyisnoob.realmfinder.core.snapshot.PhotoThumbnail;
import com.hyisnoob.realmfinder.core.math.ZoomMath;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.LinkedHashMap;

public class PhotoCaptureHelper {

    private static final Map<UUID, ResourceLocation> CAPTURED_TEXTURES = new LinkedHashMap<>(64, 0.75f, true);
    private static final int MAX_CACHED_TEXTURES = 64;
    private static ResourceLocation DEFAULT_PLACEHOLDER = null;

    private static volatile boolean pendingCapture = false;

    public static void register() {
        // Capture at the very end of world rendering, BEFORE any GUI/HUD is rendered!
        // This guarantees a 100% clean photo of the 3D world with zero UI, text, or crosshairs!
        WorldRenderEvents.END.register(context -> {
            if (pendingCapture) {
                pendingCapture = false;
                executeCleanCapture(context.camera());
            }
        });
    }

    public static void requestCapture() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.options.getCameraType().isFirstPerson()) return;
        float yaw = mc.player.getYRot();
        float nearestYaw = Math.round(yaw / 90.0f) * 90.0f;
        float deltaYaw = (yaw - nearestYaw) % 360.0f;
        if (deltaYaw > 180.0f) deltaYaw -= 360.0f;
        if (deltaYaw < -180.0f) deltaYaw += 360.0f;

        float pitch = mc.player.getXRot();

        // Snap to cardinal and level horizontal if within alignment tolerance (or holding Shift)
        if (mc.player.isShiftKeyDown() || (Math.abs(deltaYaw) < 15.0f && Math.abs(pitch) < 15.0f)) {
            mc.player.setYRot(nearestYaw);
            mc.player.setXRot(0.0f);
        }
        pendingCapture = true;
    }

    public static boolean isPendingCapture() {
        return pendingCapture;
    }

    private static Path getPhotoDir() {
        Minecraft mc = Minecraft.getInstance();
        Path dir = mc.gameDirectory.toPath().resolve("realmfinder_photos");
        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
        } catch (Exception e) {
            RealmFinder.LOGGER.error("Failed to create realmfinder_photos directory", e);
        }
        return dir;
    }

    public static ResourceLocation getTexture(UUID snapshotId) {
        return getTexture(snapshotId, null);
    }

    public static ResourceLocation getTexture(UUID snapshotId, CompoundTag photoTag) {
        if (snapshotId == null) {
            return getDefaultPlaceholder();
        }

        ResourceLocation loc = CAPTURED_TEXTURES.get(snapshotId);
        if (loc != null) {
            return loc;
        }

        // Try loading from disk
        Path filePath = getPhotoDir().resolve(snapshotId.toString() + ".png");
        if (Files.exists(filePath)) {
            try (InputStream in = Files.newInputStream(filePath)) {
                NativeImage image = NativeImage.read(in);
                DynamicTexture dynamicTexture = new DynamicTexture(image);
                ResourceLocation newLoc = RealmFinder.id("photo_" + snapshotId);
                Minecraft.getInstance().getTextureManager().register(newLoc, dynamicTexture);
                cacheTexture(snapshotId, newLoc);
                return newLoc;
            } catch (Exception e) {
                RealmFinder.LOGGER.error("Failed to load photo from disk: " + filePath, e);
            }
        }

        if (photoTag != null && photoTag.contains("Thumbnail")) {
            byte[] bytes = photoTag.getByteArray("Thumbnail");
            if (PhotoThumbnail.isValid(bytes)) {
                try {
                    NativeImage image = NativeImage.read(bytes);
                    ResourceLocation newLoc = RealmFinder.id("photo_" + snapshotId);
                    Minecraft.getInstance().getTextureManager().register(newLoc, new DynamicTexture(image));
                    cacheTexture(snapshotId, newLoc);
                    return newLoc;
                } catch (Exception e) {
                    RealmFinder.LOGGER.warn("Invalid embedded photograph thumbnail: {}", snapshotId, e);
                }
            }
        }

        return getDefaultPlaceholder();
    }

    private static ResourceLocation getDefaultPlaceholder() {
        if (DEFAULT_PLACEHOLDER == null) {
            NativeImage placeholder = new NativeImage(16, 16, false);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    if (x == 0 || x == 15 || y == 0 || y >= 13) {
                        placeholder.setPixelRGBA(x, y, 0xFFE0E0E0);
                    } else {
                        placeholder.setPixelRGBA(x, y, 0xFF222228);
                    }
                }
            }
            DynamicTexture tex = new DynamicTexture(placeholder);
            DEFAULT_PLACEHOLDER = RealmFinder.id("photo_placeholder");
            Minecraft.getInstance().getTextureManager().register(DEFAULT_PLACEHOLDER, tex);
        }
        return DEFAULT_PLACEHOLDER;
    }

    private static void cacheTexture(UUID snapshotId, ResourceLocation location) {
        CAPTURED_TEXTURES.put(snapshotId, location);
        if (CAPTURED_TEXTURES.size() > MAX_CACHED_TEXTURES) {
            UUID oldest = CAPTURED_TEXTURES.keySet().iterator().next();
            ResourceLocation oldLocation = CAPTURED_TEXTURES.remove(oldest);
            Minecraft.getInstance().getTextureManager().release(oldLocation);
        }
    }

    private static void executeCleanCapture(Camera renderCamera) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        UUID snapshotId = UUID.randomUUID();
        float fullFov = (float) mc.options.fov().get().intValue();
        float frameRatio = 0.52f;
        // Exact FOV corresponding to the 52% screen-height viewfinder window
        float croppedFov = (float) ZoomMath.croppedFov(fullFov, CameraZoom.get(), frameRatio);

        try {
            RenderTarget target = mc.getMainRenderTarget();
            int width = target.width;
            int height = target.height;

            NativeImage fullImage = new NativeImage(width, height, false);
            RenderSystem.bindTexture(target.getColorTextureId());
            fullImage.downloadTexture(0, false);
            fullImage.flipY();

            // Crop to exact viewfinder frame square (matching 52% screen height)
            int squareSize = (int) (height * frameRatio);
            int startX = (width - squareSize) / 2;
            int startY = (height - squareSize) / 2;

            NativeImage squareImage = new NativeImage(squareSize, squareSize, false);
            for (int y = 0; y < squareSize; y++) {
                for (int x = 0; x < squareSize; x++) {
                    squareImage.setPixelRGBA(x, y, fullImage.getPixelRGBA(startX + x, startY + y));
                }
            }
            fullImage.close();

            byte[] thumbnailBytes = new byte[0];
            try (NativeImage thumbnail = new NativeImage(PhotoThumbnail.SIZE, PhotoThumbnail.SIZE, false)) {
                squareImage.resizeSubRectTo(0, 0, squareSize, squareSize, thumbnail);
                byte[] encoded = thumbnail.asByteArray();
                if (PhotoThumbnail.isValid(encoded)) thumbnailBytes = encoded;
            } catch (Exception e) {
                RealmFinder.LOGGER.warn("Could not encode photograph thumbnail", e);
            }

            // Save to disk
            Path filePath = getPhotoDir().resolve(snapshotId.toString() + ".png");
            try {
                squareImage.writeToFile(filePath);
            } catch (Exception err) {
                RealmFinder.LOGGER.error("Failed to save captured photo to disk: " + filePath, err);
            }

            DynamicTexture dynamicTexture = new DynamicTexture(squareImage);
            ResourceLocation textureId = RealmFinder.id("photo_" + snapshotId);
            mc.getTextureManager().register(textureId, dynamicTexture);
            cacheTexture(snapshotId, textureId);

            // Trigger visual shutter flash
            CameraOverlayRenderer.triggerShutter();

            // Play mechanical shutter sounds
            mc.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.8f);
            mc.player.playSound(net.minecraft.sounds.SoundEvents.DISPENSER_DISPENSE, 0.7f, 1.6f);

            // Calculate intelligent auto-focus depth limiter to prevent capturing distant background terrain
            net.minecraft.world.phys.HitResult hit = mc.player.pick(36.0, 0.0f, false);
            float captureFarPlane = 36.0f;
            if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                double dist = hit.getLocation().distanceTo(mc.player.getEyePosition());
                captureFarPlane = (float) Math.min(36.0, Math.max(6.0, dist + 6.0));
            }

            // Send packet to server with exact matching cropped FOV, auto-focus depth, and camera coordinates
            ClientPlayNetworking.send(new TakePhotoPayload(
                    snapshotId, croppedFov, captureFarPlane, CameraZoom.get(),
                    renderCamera.getPosition().x, renderCamera.getPosition().y, renderCamera.getPosition().z,
                    renderCamera.getYRot(), renderCamera.getXRot(), thumbnailBytes
            ));

        } catch (Exception e) {
            RealmFinder.LOGGER.error("Failed to capture framebuffer photo", e);
        }
    }
}
