package com.hyisnoob.realmfinder.client;

import com.hyisnoob.realmfinder.client.render.CameraOverlayRenderer;
import com.hyisnoob.realmfinder.client.render.ClientPhotoTooltip;
import com.hyisnoob.realmfinder.client.render.FrustumGhostRenderer;
import com.hyisnoob.realmfinder.client.render.PhotoCaptureHelper;
import com.hyisnoob.realmfinder.client.render.ViewfinderOverlayRenderer;
import com.hyisnoob.realmfinder.common.item.CameraItem;
import com.hyisnoob.realmfinder.common.item.PhotoTooltipData;
import com.hyisnoob.realmfinder.common.item.PhotographItem;
import com.hyisnoob.realmfinder.common.network.StampPhotoPayload;
import com.hyisnoob.realmfinder.common.network.UndoPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class RealmFinderClient implements ClientModInitializer {

	private static KeyMapping undoKey;
	private static KeyMapping scaleUpKey;
	private static KeyMapping scaleDownKey;

	@Override
	public void onInitializeClient() {
		// Register clean framebuffer capture hook
		PhotoCaptureHelper.register();

		// Register Photo Album GUI Screen
		net.minecraft.client.gui.screens.MenuScreens.register(
				com.hyisnoob.realmfinder.common.menu.ModMenus.PHOTO_ALBUM,
				com.hyisnoob.realmfinder.client.gui.PhotoAlbumScreen::new
		);

		// Register Photo Stand 3D Block Entity Renderer
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
				com.hyisnoob.realmfinder.common.block.ModBlocks.PHOTO_STAND_BE,
				com.hyisnoob.realmfinder.client.render.PhotoStandRenderer::new
		);

		// Connect client capture trigger to camera (request clean capture at end of world render)
		CameraItem.setClientCaptureCallback(PhotoCaptureHelper::requestCapture);

		// Connect client stamp trigger to photograph with smooth dissolve animation
		PhotographItem.setClientStampCallback(carve -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null) {
				float yaw = mc.player.getYRot();
				float nearestYaw = Math.round(yaw / 90.0f) * 90.0f;
				float pitch = mc.player.getXRot();
				float nearestPitch = Math.abs(pitch) < 25.0f ? 0.0f : Math.round(pitch / 45.0f) * 45.0f;

				// Align client camera to exact placement angles
				mc.player.setYRot(nearestYaw);
				mc.player.setXRot(nearestPitch);

				ViewfinderOverlayRenderer.triggerStampAnimation();
				ClientPlayNetworking.send(new StampPhotoPayload(
						carve,
						mc.player.getX(), mc.player.getEyeY(), mc.player.getZ(),
						nearestYaw, nearestPitch,
						ViewfinderOverlayRenderer.getCurrentScale()
				));
			}
		});

		// Register wireframe preview (disabled for clean Viewfinder look)
		FrustumGhostRenderer.register();

		// Register HUD overlay renderer (Viewfinder Polaroid on screen)
		ViewfinderOverlayRenderer.register();

		// Register Camera Viewfinder HUD overlay (Clean bezel & shutter flash)
		CameraOverlayRenderer.register();

		// Register custom tooltip for photograph preview
		TooltipComponentCallback.EVENT.register(data -> {
			if (data instanceof PhotoTooltipData photoData) {
				return new ClientPhotoTooltip(photoData);
			}
			return null;
		});

		// Register [Z] key for Undo
		undoKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.realmfinder.undo",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_Z,
				"category.realmfinder"
		));

		// Register [ ] keys for Scale Adjustment
		scaleUpKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.realmfinder.scale_up",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_BRACKET,
				"category.realmfinder"
		));
		scaleDownKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.realmfinder.scale_down",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_LEFT_BRACKET,
				"category.realmfinder"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (undoKey.consumeClick()) {
				ClientPlayNetworking.send(new UndoPayload());
			}
			while (scaleUpKey.consumeClick()) {
				ViewfinderOverlayRenderer.cycleScale(1);
			}
			while (scaleDownKey.consumeClick()) {
				ViewfinderOverlayRenderer.cycleScale(-1);
			}
		});
	}
}