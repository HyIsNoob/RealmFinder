package com.hyisnoob.realmfinder.core.snapshot;

import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Encapsulates the entire 3D voxel capture along with metadata.
 */
public class WorldSnapshot {
    private final UUID snapshotId;
    private final long timestamp;
    private final float fov;
    private final float aspectRatio;
    private final float nearPlane;
    private final float farPlane;
    private final float originalYaw;
    private final float originalPitch;

    private final List<BlockState> palette;
    private final List<CapturedBlock> blocks;
    private final List<CapturedEntity> entities;

    public WorldSnapshot(UUID snapshotId, long timestamp, float fov, float aspectRatio,
                         float nearPlane, float farPlane, float originalYaw, float originalPitch,
                         List<BlockState> palette, List<CapturedBlock> blocks, List<CapturedEntity> entities) {
        this.snapshotId = snapshotId;
        this.timestamp = timestamp;
        this.fov = fov;
        this.aspectRatio = aspectRatio;
        this.nearPlane = nearPlane;
        this.farPlane = farPlane;
        this.originalYaw = originalYaw;
        this.originalPitch = originalPitch;
        this.palette = palette;
        this.blocks = blocks;
        this.entities = entities != null ? entities : new ArrayList<>();
    }

    public WorldSnapshot(UUID snapshotId, long timestamp, float fov, float aspectRatio,
                         float nearPlane, float farPlane, float originalYaw, float originalPitch,
                         List<BlockState> palette, List<CapturedBlock> blocks) {
        this(snapshotId, timestamp, fov, aspectRatio, nearPlane, farPlane, originalYaw, originalPitch, palette, blocks, new ArrayList<>());
    }

    public static WorldSnapshot createNew(UUID snapshotId, float fov, float aspectRatio,
                                          float nearPlane, float farPlane,
                                          float originalYaw, float originalPitch) {
        return new WorldSnapshot(snapshotId, System.currentTimeMillis(), fov, aspectRatio,
                nearPlane, farPlane, originalYaw, originalPitch,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    public UUID getSnapshotId() { return snapshotId; }
    public long getTimestamp() { return timestamp; }
    public float getFov() { return fov; }
    public float getAspectRatio() { return aspectRatio; }
    public float getNearPlane() { return nearPlane; }
    public float getFarPlane() { return farPlane; }
    public float getOriginalYaw() { return originalYaw; }
    public float getOriginalPitch() { return originalPitch; }

    public List<BlockState> getPalette() { return palette; }
    public List<CapturedBlock> getBlocks() { return blocks; }
    public List<CapturedEntity> getEntities() { return entities; }

    public int getOrAddPaletteIndex(BlockState state) {
        for (int i = 0; i < palette.size(); i++) {
            if (palette.get(i).equals(state)) {
                return i;
            }
        }
        palette.add(state);
        return palette.size() - 1;
    }

    public BlockState getBlockState(int paletteIndex) {
        if (paletteIndex >= 0 && paletteIndex < palette.size()) {
            return palette.get(paletteIndex);
        }
        return null;
    }

    public int getBlockCount() {
        return blocks.size();
    }

    public int getEntityCount() {
        return entities.size();
    }
}
