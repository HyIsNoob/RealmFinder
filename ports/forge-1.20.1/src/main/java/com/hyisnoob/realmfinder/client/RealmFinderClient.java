package com.hyisnoob.realmfinder.client;

import com.hyisnoob.realmfinder.client.render.CameraOverlayRenderer;
import com.hyisnoob.realmfinder.client.render.ClientPhotoTooltip;
import com.hyisnoob.realmfinder.client.render.FrustumGhostRenderer;
import com.hyisnoob.realmfinder.client.render.PhotoCaptureHelper;
import com.hyisnoob.realmfinder.client.render.ViewfinderOverlayRenderer;
import com.hyisnoob.realmfinder.client.gui.RealmFinderSettingsScreen;
import com.hyisnoob.realmfinder.common.item.CameraItem;
import com.hyisnoob.realmfinder.common.item.PhotoTooltipData;
import com.hyisnoob.realmfinder.common.item.PhotographItem;
import com.hyisnoob.realmfinder.common.network.StampPhotoPayload;
import com.hyisnoob.realmfinder.common.network.StampResultPayload;
import com.hyisnoob.realmfinder.common.network.UndoPayload;
import com.hyisnoob.realmfinder.common.network.SettingsSyncPayload;
import com.hyisnoob.realmfinder.common.network.ModPackets;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class RealmFinderClient {

	private static KeyMapping undoKey;
	private static KeyMapping scaleUpKey;
	private static KeyMapping scaleDownKey;
	private static KeyMapping settingsKey;

	public static void register(IEventBus modBus) {
		modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(RealmFinderClient::initialize));
		modBus.addListener(RealmFinderClient::registerKeys);
		modBus.addListener((RegisterClientTooltipComponentFactoriesEvent event) ->
				event.register(PhotoTooltipData.class, ClientPhotoTooltip::new));
		MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
			if (event.phase == TickEvent.Phase.END) onTick(Minecraft.getInstance());
		});
	}

	public static void receiveSettings(SettingsSyncPayload payload) {
		Minecraft client = Minecraft.getInstance();
		client.execute(() -> {
			ServerSettings.accept(payload.json(), payload.editable());
			if (client.screen instanceof RealmFinderSettingsScreen screen) screen.applyServerSettings();
		});
	}

	public static void receiveStampResult() {
		Minecraft.getInstance().execute(() -> {
			ViewfinderOverlayRenderer.triggerStampAnimation();
			CameraOverlayRenderer.triggerShutter();
		});
	}

	private static void initialize() {
		ClientPreferences.load();
		net.minecraft.client.gui.screens.MenuScreens.register(
				com.hyisnoob.realmfinder.common.menu.ModMenus.PHOTO_ALBUM,
				com.hyisnoob.realmfinder.client.gui.PhotoAlbumScreen::new);
		// Register clean framebuffer capture hook
		PhotoCaptureHelper.register();

		// Register Photo Stand 3D Block Entity Renderer
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
				com.hyisnoob.realmfinder.common.block.ModBlocks.PHOTO_STAND_BE,
				com.hyisnoob.realmfinder.client.render.PhotoStandRenderer::new
		);

		// Connect client capture trigger to camera (request clean capture at end of world render)
		CameraItem.setClientCaptureCallback(PhotoCaptureHelper::requestCapture);

		// Connect client stamp trigger to photograph with smooth dissolve animation
		PhotographItem.setClientStampCallback((carve, hand, snapshotId) -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null && mc.options.getCameraType().isFirstPerson()) {
				float yaw = mc.player.getYRot();
				float nearestYaw = Math.round(yaw / 90.0f) * 90.0f;
				float pitch = mc.player.getXRot();
				float nearestPitch = Math.abs(pitch) < 25.0f ? 0.0f : Math.round(pitch / 45.0f) * 45.0f;

				// Align client camera to exact placement angles
				mc.player.setYRot(nearestYaw);
				mc.player.setXRot(nearestPitch);

				ModPackets.sendToServer(new StampPhotoPayload(
						carve,
						hand == net.minecraft.world.InteractionHand.OFF_HAND, snapshotId,
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
	}

	private static void registerKeys(RegisterKeyMappingsEvent event) {
		undoKey = new KeyMapping(
				"key.realmfinder.undo",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_Z,
				"category.realmfinder"
		);
		event.register(undoKey);
		settingsKey = new KeyMapping(
				"key.realmfinder.settings", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, "category.realmfinder");
		event.register(settingsKey);

		// Register [ ] keys for Scale Adjustment
		scaleUpKey = new KeyMapping(
				"key.realmfinder.scale_up",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_BRACKET,
				"category.realmfinder"
		);
		event.register(scaleUpKey);
		scaleDownKey = new KeyMapping(
				"key.realmfinder.scale_down",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_LEFT_BRACKET,
				"category.realmfinder"
		);
		event.register(scaleDownKey);
	}

	private static void onTick(Minecraft client) {
			while (settingsKey.consumeClick()) {
				if (client.player != null && client.screen == null) {
					ServerSettings.reset();
					client.setScreen(new RealmFinderSettingsScreen());
				}
			}
			while (undoKey.consumeClick()) {
				if (client.player == null || client.screen != null) continue;
				if (ClientPreferences.get().confirmBeforeUndo) {
					client.setScreen(new ConfirmScreen(confirmed -> {
						client.setScreen(null);
						if (confirmed) ModPackets.sendToServer(new UndoPayload());
					}, Component.translatable("screen.realmfinder.undo_confirm"),
						Component.translatable("screen.realmfinder.undo_confirm_detail")));
				} else ModPackets.sendToServer(new UndoPayload());
			}
			while (scaleUpKey.consumeClick()) {
				ViewfinderOverlayRenderer.cycleScale(1);
			}
			while (scaleDownKey.consumeClick()) {
				ViewfinderOverlayRenderer.cycleScale(-1);
			}
	}
}
