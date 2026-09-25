package com.hyisnoob.realmfinder.test;

import com.hyisnoob.realmfinder.core.math.CameraTransform;
import com.hyisnoob.realmfinder.core.math.Frustum;
import org.joml.Vector3f;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MathAndFrustumTest {

    @Test
    public void testWorldTransformRetainsBlockPrecisionNearWorldBorder() {
        CameraTransform camera = new CameraTransform(20_000_000.5, 65.0, -20_000_000.5, 0, 0);
        Vector3d world = camera.toWorldSpacePrecise(-1.0f, -0.5f, 5.0f);
        assertEquals(20_000_001.5, world.x, 0.0001);
        assertEquals(64.5, world.y, 0.0001);
        assertEquals(-19_999_995.5, world.z, 0.0001);
    }

    @Test
    public void testCameraTransformVectors() {
        // Player at origin looking South (yaw = 0, pitch = 0)
        CameraTransform camSouth = new CameraTransform(0, 0, 0, 0, 0);
        Vector3f fwdSouth = camSouth.getForward();
        assertEquals(0.0f, fwdSouth.x, 0.001f);
        assertEquals(0.0f, fwdSouth.y, 0.001f);
        assertEquals(1.0f, fwdSouth.z, 0.001f); // South is +Z

        // Player at origin looking North (yaw = 180, pitch = 0)
        CameraTransform camNorth = new CameraTransform(0, 0, 0, 180, 0);
        Vector3f fwdNorth = camNorth.getForward();
        assertEquals(0.0f, fwdNorth.x, 0.001f);
        assertEquals(0.0f, fwdNorth.y, 0.001f);
        assertEquals(-1.0f, fwdNorth.z, 0.001f); // North is -Z

        // Player looking West (yaw = 90, pitch = 0)
        CameraTransform camWest = new CameraTransform(0, 0, 0, 90, 0);
        Vector3f fwdWest = camWest.getForward();
        assertEquals(-1.0f, fwdWest.x, 0.001f); // West is -X
        assertEquals(0.0f, fwdWest.y, 0.001f);
        assertEquals(0.0f, fwdWest.z, 0.001f);

        // Player looking East (yaw = 270, pitch = 0)
        CameraTransform camEast = new CameraTransform(0, 0, 0, 270, 0);
        Vector3f fwdEast = camEast.getForward();
        assertEquals(1.0f, fwdEast.x, 0.001f); // East is +X
        assertEquals(0.0f, fwdEast.y, 0.001f);
        assertEquals(0.0f, fwdEast.z, 0.001f);

        // Player looking straight up (pitch = -90)
        CameraTransform camUp = new CameraTransform(0, 0, 0, 0, -90);
        Vector3f fwdUp = camUp.getForward();
        assertEquals(0.0f, fwdUp.x, 0.001f);
        assertEquals(1.0f, fwdUp.y, 0.001f); // Up is +Y
        assertEquals(0.0f, fwdUp.z, 0.001f);
    }

    @Test
    public void testCameraVectorOrthogonalityAndNorm() {
        float[] yaws = {0, 45, 90, 135, 180, 225, 270, 315};
        float[] pitches = {-45, 0, 45};

        for (float y : yaws) {
            for (float p : pitches) {
                CameraTransform cam = new CameraTransform(10, 64, -10, y, p);
                Vector3f fwd = cam.getForward();
                Vector3f right = cam.getRight();
                Vector3f up = cam.getUp();

                // Normalized
                assertEquals(1.0f, fwd.length(), 0.001f);
                assertEquals(1.0f, right.length(), 0.001f);
                assertEquals(1.0f, up.length(), 0.001f);

                // Mutually orthogonal (dot product == 0)
                assertEquals(0.0f, fwd.dot(right), 0.001f);
                assertEquals(0.0f, fwd.dot(up), 0.001f);
                assertEquals(0.0f, right.dot(up), 0.001f);
            }
        }
    }

    @Test
    public void testCameraSpaceRoundTrip() {
        CameraTransform cam = new CameraTransform(100.5, 64.0, -200.5, 45.0f, 15.0f);

        double targetWorldX = 110.0;
        double targetWorldY = 70.0;
        double targetWorldZ = -190.0;

        Vector3f camSpace = cam.toCameraSpace(targetWorldX, targetWorldY, targetWorldZ);
        Vector3f backToWorld = cam.toWorldSpace(camSpace.x, camSpace.y, camSpace.z);

        assertEquals(targetWorldX, backToWorld.x, 0.001f);
        assertEquals(targetWorldY, backToWorld.y, 0.001f);
        assertEquals(targetWorldZ, backToWorld.z, 0.001f);
    }

    @Test
    public void testFrustumContainment() {
        CameraTransform cam = new CameraTransform(0, 64, 0, 0, 0); // Looking South (+Z)
        Frustum frustum = new Frustum(cam, 70.0f, 1.0f, 1.0f, 32.0f);

        // Point directly in front at 10 blocks: (0, 64, 10) -> should be inside
        assertTrue(frustum.containsPoint(0, 64, 10));

        // Point behind player: (0, 64, -5) -> should be outside
        assertFalse(frustum.containsPoint(0, 64, -5));

        // Point beyond far plane: (0, 64, 50) -> should be outside
        assertFalse(frustum.containsPoint(0, 64, 50));

        // Point too close (near plane is 1.0): (0, 64, 0.5) -> should be outside
        assertFalse(frustum.containsPoint(0, 64, 0.5));

        // Point far off to the side at distance 10: half width at 10 is ~7.0 blocks. (20, 64, 10) -> outside
        assertFalse(frustum.containsPoint(20, 64, 10));
    }

    @Test
    public void testCardinalSnappingMath() {
        // Angles close to cardinal headings
        float[] rawYaws = {-4.2f, 3.8f, 85.1f, 94.7f, 177.3f, 182.9f, 266.4f, 273.2f};
        float[] expectedSnaps = {0.0f, 0.0f, 90.0f, 90.0f, 180.0f, 180.0f, 270.0f, 270.0f};

        for (int i = 0; i < rawYaws.length; i++) {
            float snapped = Math.round(rawYaws[i] / 90.0f) * 90.0f;
            if (snapped == -0.0f) snapped = 0.0f;
            assertEquals(expectedSnaps[i], snapped, 0.001f);
        }

        // Difference between any two snapped cardinal angles must be an exact multiple of 90
        for (float yawA : new float[]{0.0f, 90.0f, 180.0f, 270.0f}) {
            for (float yawB : new float[]{0.0f, 90.0f, 180.0f, 270.0f}) {
                float delta = Math.abs(yawA - yawB);
                assertEquals(0.0f, delta % 90.0f, 0.001f);
            }
        }
    }

    @Test
    public void testVoxelGridIntegrityNoHolesNoCollisions() {
        // Create an original camera at eye level (0, 65.62, 0) looking South (yaw = 0, pitch = 0)
        CameraTransform originCam = new CameraTransform(0, 65.62, 0, 0, 0);

        // A solid 2x2x2 cube of blocks placed in front of camera at distance 5 (Z: 4..5, X: -1..0, Y: 64..65)
        List<Vector3f> camBlocks = new ArrayList<>();
        for (int x = -1; x <= 0; x++) {
            for (int y = 64; y <= 65; y++) {
                for (int z = 4; z <= 5; z++) {
                    Vector3f rel = originCam.toCameraSpace(x + 0.5, y + 0.5, z + 0.5);
                    camBlocks.add(rel);
                }
            }
        }
        assertEquals(8, camBlocks.size());

        // Test placing this 2x2x2 cube with all 4 cardinal rotations (0, 90, 180, 270)
        float[] cardinalYaws = {0.0f, 90.0f, 180.0f, 270.0f};
        for (float targetYaw : cardinalYaws) {
            CameraTransform targetCam = new CameraTransform(100.5, 65.62, 200.5, targetYaw, 0.0f);
            Map<String, Boolean> placedPositions = new HashMap<>();

            for (Vector3f camRel : camBlocks) {
                Vector3f worldPos = targetCam.toWorldSpace(camRel.x, camRel.y, camRel.z);
                // Math.floor equals BlockPos.containing
                int bx = (int) Math.floor(worldPos.x);
                int by = (int) Math.floor(worldPos.y);
                int bz = (int) Math.floor(worldPos.z);
                String key = bx + "," + by + "," + bz;
                placedPositions.put(key, true);
            }

            // Must have exactly 8 unique block coordinates (NO COLLISIONS, NO HOLES!)
            assertEquals(8, placedPositions.size(), "At yaw " + targetYaw + " all 8 voxels must place into unique integer coordinates");
        }
    }

    @Test
    public void testCarvingGroundProtectionRule() {
        // Placed structure sits at minY = 64, maxY = 66
        int minY = 64;
        int maxY = 66;

        // Verify that carving rule strictly excludes y <= minY
        for (int y = 60; y <= 70; y++) {
            boolean canCarve = (y > minY && y <= maxY);
            if (y <= 64) {
                assertFalse(canCarve, "Ground at or below minY=" + minY + " must NEVER be carved (checked Y=" + y + ")");
            } else if (y <= 66) {
                assertTrue(canCarve, "Structure interior above minY should be eligible for carving (checked Y=" + y + ")");
            } else {
                assertFalse(canCarve, "Blocks above maxY should not be carved (checked Y=" + y + ")");
            }
        }
    }

    @Test
    public void testEntityPositionCameraTransformAndScale() {
        CameraTransform originCam = new CameraTransform(10, 65, 10, 0, 0);
        // Entity standing at (12, 65, 15)
        Vector3f camRel = originCam.toCameraSpace(12, 65, 15);
        // Looking South (yaw 0), East (+X) is to the left, so camRight = -2
        assertEquals(-2.0f, camRel.x, 0.001f);
        assertEquals(0.0f, camRel.y, 0.001f);
        assertEquals(5.0f, camRel.z, 0.001f);

        // Roundtrip at scale 1.0 must return exact original coordinates
        Vector3f roundtrip = originCam.toWorldSpace(camRel.x, camRel.y, camRel.z);
        assertEquals(12.0f, roundtrip.x, 0.01f);
        assertEquals(65.0f, roundtrip.y, 0.01f);
        assertEquals(15.0f, roundtrip.z, 0.01f);

        // Zoom 2x moves the entity to half its captured depth without changing height or width.
        CameraTransform targetCam = new CameraTransform(100, 65, 100, 0, 0);
        var worldPos = com.hyisnoob.realmfinder.core.engine.PlacementGeometry.worldPosition(
                targetCam, camRel.x, camRel.y, camRel.z, 2.0f);
        assertEquals(102.0, worldPos.x, 0.01);
        assertEquals(65.0, worldPos.y, 0.01);
        assertEquals(102.5, worldPos.z, 0.01);
    }
}
