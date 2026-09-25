package com.hyisnoob.realmfinder.core.math;

public final class ZoomMath {
    private ZoomMath() {}

    public static double viewFov(double baseFov, double zoom) {
        return Math.toDegrees(2.0 * Math.atan(Math.tan(Math.toRadians(baseFov * 0.5)) / zoom));
    }

    public static double croppedFov(double baseFov, double zoom, double frameFraction) {
        return Math.toDegrees(2.0 * Math.atan(frameFraction
                * Math.tan(Math.toRadians(baseFov * 0.5)) / zoom));
    }
}
