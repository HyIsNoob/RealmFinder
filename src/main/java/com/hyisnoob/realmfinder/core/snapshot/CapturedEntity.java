package com.hyisnoob.realmfinder.core.snapshot;

import net.minecraft.nbt.CompoundTag;

/**
 * Represents a single captured entity (mob, animal, armor stand, item frame, etc.)
 * positioned relative to the camera coordinate frame.
 */
public class CapturedEntity {
    private final float camX;
    private final float camY;
    private final float camZ;
    private final float yaw;
    private final float pitch;
    private final String entityTypeId;
    private final CompoundTag entityNbt;

    public CapturedEntity(float camX, float camY, float camZ, float yaw, float pitch,
                          String entityTypeId, CompoundTag entityNbt) {
        this.camX = camX;
        this.camY = camY;
        this.camZ = camZ;
        this.yaw = yaw;
        this.pitch = pitch;
        this.entityTypeId = entityTypeId;
        this.entityNbt = entityNbt;
    }

    public float getCamX() { return camX; }
    public float getCamY() { return camY; }
    public float getCamZ() { return camZ; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public String getEntityTypeId() { return entityTypeId; }
    public CompoundTag getEntityNbt() { return entityNbt != null ? entityNbt.copy() : new CompoundTag(); }
}
