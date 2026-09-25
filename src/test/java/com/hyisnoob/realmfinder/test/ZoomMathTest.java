package com.hyisnoob.realmfinder.test;

import com.hyisnoob.realmfinder.core.engine.PlacementGeometry;
import com.hyisnoob.realmfinder.core.math.ZoomMath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ZoomMathTest {
    @Test
    void captureFovMatchesZoomedViewfinderCrop() {
        double normal = ZoomMath.croppedFov(70, 1, 0.52);
        double zoomed = ZoomMath.croppedFov(70, 2, 0.52);
        assertTrue(zoomed < normal);
        assertEquals(Math.tan(Math.toRadians(normal / 2)) / 2,
                Math.tan(Math.toRadians(zoomed / 2)), 1e-6);
    }

    @Test
    void zoomedCaptureAtFocusProjectsToSamePhotoSizeAfterPlacement() {
        double capturedFov = ZoomMath.croppedFov(70, 2, 0.52);
        double normalPhotoFov = ZoomMath.croppedFov(70, 1, 0.52);
        double capturedDepth = 10;
        double placedDepth = capturedDepth - PlacementGeometry.cameraZoomOffset(2, 16);
        assertEquals(Math.tan(Math.toRadians(capturedFov / 2)) * capturedDepth,
                Math.tan(Math.toRadians(normalPhotoFov / 2)) * placedDepth, 1e-6);
    }
}
