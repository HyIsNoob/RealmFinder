package com.hyisnoob.realmfinder.client.mixin;

import com.hyisnoob.realmfinder.client.render.ViewfinderOverlayRenderer;
import com.hyisnoob.realmfinder.common.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseScrollMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long windowPointer, double xOffset, double yOffset, CallbackInfo ci) {
        if (this.minecraft.player != null && windowPointer == this.minecraft.getWindow().getWindow()) {
            boolean holdingPhoto = this.minecraft.player.getMainHandItem().is(ModItems.PHOTOGRAPH)
                    || this.minecraft.player.getOffhandItem().is(ModItems.PHOTOGRAPH);

            if (holdingPhoto && (this.minecraft.player.isShiftKeyDown() || Screen.hasControlDown() || Screen.hasAltDown())) {
                if (yOffset != 0) {
                    int dir = yOffset > 0 ? 1 : -1;
                    ViewfinderOverlayRenderer.cycleScale(dir);
                    ci.cancel();
                }
            }
        }
    }
}
