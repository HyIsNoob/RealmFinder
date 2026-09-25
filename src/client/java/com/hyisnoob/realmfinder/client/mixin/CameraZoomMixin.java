package com.hyisnoob.realmfinder.client.mixin;

import com.hyisnoob.realmfinder.client.CameraZoom;
import com.hyisnoob.realmfinder.common.item.ModItems;
import com.hyisnoob.realmfinder.core.math.ZoomMath;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class CameraZoomMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void realmfinder$cameraZoom(Camera camera, float partialTick, boolean useFovSetting,
                                        CallbackInfoReturnable<Double> result) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !mc.options.getCameraType().isFirstPerson()) return;
        if (!mc.player.getMainHandItem().is(ModItems.CAMERA)
                && !mc.player.getOffhandItem().is(ModItems.CAMERA)) return;
        int zoom = CameraZoom.get();
        if (zoom > 1) result.setReturnValue(ZoomMath.viewFov(result.getReturnValue(), zoom));
    }
}
