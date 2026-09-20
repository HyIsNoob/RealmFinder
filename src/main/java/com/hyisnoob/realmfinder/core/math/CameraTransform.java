package com.hyisnoob.realmfinder.core.math;

import org.joml.Vector3f;

/**
 * Encapsulates camera orientation and coordinates in Minecraft's coordinate system.
 * X = East (+X) / West (-X)
 * Y = Up (+Y) / Down (-Y)
 * Z = South (+Z) / North (-Z)
 */
public class CameraTransform {
    private final double eyeX;
    private final double eyeY;
    private final double eyeZ;
    private final float yaw;
    private final float pitch;

    private final Vector3f forward;
    private final Vector3f right;
    private final Vector3f up;

    public CameraTransform(double eyeX, double eyeY, double eyeZ, float yaw, float pitch) {
        this.eyeX = eyeX;
        this.eyeY = eyeY;
        this.eyeZ = eyeZ;
        this.yaw = yaw;
        this.pitch = pitch;

        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);

        // Forward vector
        float fx = (float) (-Math.sin(yawRad) * Math.cos(pitchRad));
        float fy = (float) (-Math.sin(pitchRad));
        float fz = (float) (Math.cos(yawRad) * Math.cos(pitchRad));
        this.forward = new Vector3f(fx, fy, fz).normalize();

        // Right vector (horizontal, perpendicular to yaw)
        // Yaw 0 (South): Right is West (-X)
        // Yaw 180 (North): Right is East (+X)
        float rx = (float) (-Math.cos(yawRad));
        float ry = 0.0f;
        float rz = (float) (-Math.sin(yawRad));
        this.right = new Vector3f(rx, ry, rz).normalize();

        // Up vector: Right x Forward
        this.up = new Vector3f();
        this.right.cross(this.forward, this.up);
        this.up.normalize();
    }

    public double getEyeX() { return eyeX; }
    public double getEyeY() { return eyeY; }
    public double getEyeZ() { return eyeZ; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }

    public Vector3f getForward() { return new Vector3f(forward); }
    public Vector3f getRight() { return new Vector3f(right); }
    public Vector3f getUp() { return new Vector3f(up); }

    /**
     * Converts a world coordinate into camera-space relative coordinate (xRight, yUp, zForward).
     */
    public Vector3f toCameraSpace(double wx, double wy, double wz) {
        double dx = wx - eyeX;
        double dy = wy - eyeY;
        double dz = wz - eyeZ;

        float camRight = (float) (dx * right.x + dy * right.y + dz * right.z);
        float camUp = (float) (dx * up.x + dy * up.y + dz * up.z);
        float camForward = (float) (dx * forward.x + dy * forward.y + dz * forward.z);

        return new Vector3f(camRight, camUp, camForward);
    }

    /**
     * Converts camera-space coordinates (xRight, yUp, zForward) back into world coordinates.
     */
    public Vector3f toWorldSpace(float camRight, float camUp, float camForward) {
        float wx = (float) eyeX + camRight * right.x + camUp * up.x + camForward * forward.x;
        float wy = (float) eyeY + camRight * right.y + camUp * up.y + camForward * forward.y;
        float wz = (float) eyeZ + camRight * right.z + camUp * up.z + camForward * forward.z;
        return new Vector3f(wx, wy, wz);
    }
}
