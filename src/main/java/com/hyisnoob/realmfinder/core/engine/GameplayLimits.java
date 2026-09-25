package com.hyisnoob.realmfinder.core.engine;

public final class GameplayLimits {
    public static final int MAX_CAPTURE_BLOCKS = 16384;
    public static final int MAX_PLACED_BLOCKS = 32768;
    public static final int MAX_CAPTURED_ENTITIES = 16;
    public static final int MAX_COMPRESSED_BLOCK_BYTES = 1024 * 1024;
    public static final int MAX_BLOCK_ENTITY_BYTES = 64 * 1024;
    public static final int MAX_ENTITY_DATA_BYTES = 128 * 1024;
    public static final int MAX_TOTAL_ENTITY_DATA_BYTES = 512 * 1024;
    public static final double MAX_POSE_DISTANCE_SQUARED = 9.0;

    private GameplayLimits() {}

    public static boolean validCapture(float fov, float farPlane, double x, double y, double z,
                                       float yaw, float pitch, double playerX, double playerY, double playerZ) {
        return Float.isFinite(fov) && fov >= 1 && fov <= 100
                && Float.isFinite(farPlane) && farPlane >= 1 && farPlane <= 36
                && validPose(x, y, z, yaw, pitch, playerX, playerY, playerZ);
    }

    public static boolean validStamp(float scale, double x, double y, double z,
                                     float yaw, float pitch, double playerX, double playerY, double playerZ) {
        return (scale == 0.5f || scale == 1.0f || scale == 2.0f || scale == 3.0f)
                && validPose(x, y, z, yaw, pitch, playerX, playerY, playerZ);
    }

    private static boolean validPose(double x, double y, double z, float yaw, float pitch,
                                     double playerX, double playerY, double playerZ) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Float.isFinite(yaw) || !Float.isFinite(pitch) || Math.abs(yaw) > 36000 || Math.abs(pitch) > 90) {
            return false;
        }
        double dx = x - playerX;
        double dy = y - playerY;
        double dz = z - playerZ;
        return dx * dx + dy * dy + dz * dz <= MAX_POSE_DISTANCE_SQUARED;
    }
}
