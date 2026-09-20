package com.hyisnoob.realmfinder.core.math;

import org.joml.Vector3f;

/**
 * Geometric viewing frustum (pyramid with near/far clipping planes)
 * used for capturing and carving blocks in the Viewfinder mechanic.
 */
public class Frustum {
    private final CameraTransform transform;
    private final float fovDegrees;
    private final float aspectRatio;
    private final float nearPlane;
    private final float farPlane;

    private final float tanHalfFov;

    public Frustum(CameraTransform transform, float fovDegrees, float aspectRatio, float nearPlane, float farPlane) {
        this.transform = transform;
        this.fovDegrees = fovDegrees;
        this.aspectRatio = aspectRatio;
        this.nearPlane = nearPlane;
        this.farPlane = farPlane;
        this.tanHalfFov = (float) Math.tan(Math.toRadians(fovDegrees * 0.5f));
    }

    public CameraTransform getTransform() {
        return transform;
    }

    public float getFovDegrees() {
        return fovDegrees;
    }

    public float getAspectRatio() {
        return aspectRatio;
    }

    public float getNearPlane() {
        return nearPlane;
    }

    public float getFarPlane() {
        return farPlane;
    }

    /**
     * Checks whether a 3D world coordinate is inside the frustum.
     */
    public boolean containsPoint(double wx, double wy, double wz) {
        Vector3f cam = transform.toCameraSpace(wx, wy, wz);
        if (cam.z < nearPlane || cam.z > farPlane) {
            return false;
        }

        float halfH = cam.z * tanHalfFov;
        float halfW = halfH * aspectRatio;

        return Math.abs(cam.x) <= halfW && Math.abs(cam.y) <= halfH;
    }

    /**
     * Checks whether a Minecraft block at (bx, by, bz) overlaps with the frustum.
     * Uses block center and corner checks for accurate clipping.
     */
    public boolean containsBlock(int bx, int by, int bz) {
        // Test block center first (fast path)
        if (containsPoint(bx + 0.5, by + 0.5, bz + 0.5)) {
            return true;
        }

        // Test corners
        double[][] corners = {
            {bx, by, bz},
            {bx + 1, by, bz},
            {bx, by + 1, bz},
            {bx + 1, by + 1, bz},
            {bx, by, bz + 1},
            {bx + 1, by, bz + 1},
            {bx, by + 1, bz + 1},
            {bx + 1, by + 1, bz + 1}
        };

        for (double[] corner : corners) {
            if (containsPoint(corner[0], corner[1], corner[2])) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the 4 corners of the frustum plane at the given distance in world coordinates.
     * Order: Top-Left, Top-Right, Bottom-Right, Bottom-Left
     */
    public Vector3f[] getCornersAtDistance(float distance) {
        float halfH = distance * tanHalfFov;
        float halfW = halfH * aspectRatio;

        return new Vector3f[] {
            transform.toWorldSpace(-halfW,  halfH, distance), // TL
            transform.toWorldSpace( halfW,  halfH, distance), // TR
            transform.toWorldSpace( halfW, -halfH, distance), // BR
            transform.toWorldSpace(-halfW, -halfH, distance)  // BL
        };
    }
}
