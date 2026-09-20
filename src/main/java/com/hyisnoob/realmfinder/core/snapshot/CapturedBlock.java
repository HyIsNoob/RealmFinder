package com.hyisnoob.realmfinder.core.snapshot;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a single captured block relative to the camera frame.
 */
public class CapturedBlock {
    private final float camX;
    private final float camY;
    private final float camZ;
    private final int paletteIndex;
    @Nullable
    private final CompoundTag blockEntityData;

    public CapturedBlock(float camX, float camY, float camZ, int paletteIndex, @Nullable CompoundTag blockEntityData) {
        this.camX = camX;
        this.camY = camY;
        this.camZ = camZ;
        this.paletteIndex = paletteIndex;
        this.blockEntityData = blockEntityData;
    }

    public float getCamX() { return camX; }
    public float getCamY() { return camY; }
    public float getCamZ() { return camZ; }
    public int getPaletteIndex() { return paletteIndex; }

    @Nullable
    public CompoundTag getBlockEntityData() {
        return blockEntityData != null ? blockEntityData.copy() : null;
    }
}
