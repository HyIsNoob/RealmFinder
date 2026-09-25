package com.hyisnoob.realmfinder.core.engine;

import com.hyisnoob.realmfinder.core.math.CameraTransform;
import com.hyisnoob.realmfinder.core.snapshot.CapturedBlock;
import com.hyisnoob.realmfinder.core.snapshot.WorldSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/** The voxel coordinates used by both the world preview and the server stamp. */
public final class PlacementGeometry {
    private PlacementGeometry() {}

    public record Plan(Map<BlockPos, BlockState> blocks, Map<BlockPos, CompoundTag> blockEntities,
                       Set<BlockPos> carvePositions, CameraTransform camera,
                       int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {}

    public static Plan build(WorldSnapshot snapshot, CameraTransform target, float scale, int minHeight, int maxHeight) {
        if (snapshot == null || snapshot.getBlocks().size() > GameplayLimits.MAX_CAPTURE_BLOCKS
                || !(scale == 0.5f || scale == 1 || scale == 2 || scale == 3)) return null;

        float yaw = Math.round(target.getYaw() / 90.0f) * 90.0f;
        float pitch = Math.round(target.getPitch() / 45.0f) * 45.0f;
        if (Math.abs(pitch) < 25) pitch = 0;
        CameraTransform camera = new CameraTransform(target.getEyeX(), target.getEyeY(), target.getEyeZ(), yaw, pitch);
        Rotation rotation = rotation(yaw - snapshot.getOriginalYaw());
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        Map<BlockPos, CompoundTag> blockEntities = new HashMap<>();
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (CapturedBlock captured : snapshot.getBlocks()) {
            BlockState original = snapshot.getBlockState(captured.getPaletteIndex());
            if (original == null || original.isAir()) continue;
            BlockState state = original.rotate(rotation);
            Vector3d world = worldPosition(camera, captured.getCamX(), captured.getCamY(), captured.getCamZ(),
                    scale, snapshot.getCaptureZoom(), snapshot.getFarPlane());
            if (!Double.isFinite(world.x) || !Double.isFinite(world.y) || !Double.isFinite(world.z)) return null;
            BlockPos pos = BlockPos.containing(world.x, world.y, world.z);
            if (pos.getY() < minHeight || pos.getY() >= maxHeight) continue;
            blocks.put(pos, state);
            CompoundTag data = captured.getBlockEntityData();
            if (data != null && (state.getBlock() instanceof SignBlock
                    || state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock)) blockEntities.put(pos, data);
            else blockEntities.remove(pos);
            minX = Math.min(minX, pos.getX()); maxX = Math.max(maxX, pos.getX());
            minY = Math.min(minY, pos.getY()); maxY = Math.max(maxY, pos.getY());
            minZ = Math.min(minZ, pos.getZ()); maxZ = Math.max(maxZ, pos.getZ());
        }
        removeSeparatedMultiblocks(blocks);
        blockEntities.keySet().retainAll(blocks.keySet());
        minX = Integer.MAX_VALUE; maxX = Integer.MIN_VALUE;
        minY = Integer.MAX_VALUE; maxY = Integer.MIN_VALUE;
        minZ = Integer.MAX_VALUE; maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : blocks.keySet()) {
            minX = Math.min(minX, pos.getX()); maxX = Math.max(maxX, pos.getX());
            minY = Math.min(minY, pos.getY()); maxY = Math.max(maxY, pos.getY());
            minZ = Math.min(minZ, pos.getZ()); maxZ = Math.max(maxZ, pos.getZ());
        }
        return new Plan(blocks, blockEntities, interiorCarvePositions(blocks), camera,
                minX, maxX, minY, maxY, minZ, maxZ);
    }

    /**
     * Finds empty cells inside each occupied X/Z column of the projected structure.
     * Starting above the lowest photographed block preserves the terrain beneath the
     * structure while clearing trees and other obstructions between its floor and roof.
     */
    public static Set<BlockPos> interiorCarvePositions(Map<BlockPos, BlockState> blocks) {
        Map<Long, int[]> columnSpans = new HashMap<>();
        for (BlockPos pos : blocks.keySet()) {
            long column = BlockPos.asLong(pos.getX(), 0, pos.getZ());
            int[] span = columnSpans.computeIfAbsent(column, ignored -> new int[] { pos.getY(), pos.getY() });
            span[0] = Math.min(span[0], pos.getY());
            span[1] = Math.max(span[1], pos.getY());
        }

        Set<BlockPos> carvePositions = new HashSet<>();
        for (Map.Entry<Long, int[]> entry : columnSpans.entrySet()) {
            BlockPos column = BlockPos.of(entry.getKey());
            int[] span = entry.getValue();
            for (int y = span[0] + 1; y <= span[1]; y++) {
                BlockPos pos = new BlockPos(column.getX(), y, column.getZ());
                if (!blocks.containsKey(pos)) {
                    carvePositions.add(pos);
                    if (carvePositions.size() > GameplayLimits.MAX_PLACED_BLOCKS) return carvePositions;
                }
            }
        }
        return carvePositions;
    }

    /** Remove incomplete two-block structures before Minecraft's neighbor updates break them. */
    public static void removeSeparatedMultiblocks(Map<BlockPos, BlockState> blocks) {
        Set<BlockPos> invalid = new HashSet<>();
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            BlockState state = entry.getValue();
            if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                boolean lower = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
                BlockPos matePos = lower ? entry.getKey().above() : entry.getKey().below();
                BlockState mate = blocks.get(matePos);
                if (mate == null || mate.getBlock() != state.getBlock()
                        || !mate.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                        || mate.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                    invalid.add(entry.getKey());
                }
            } else if (state.getBlock() instanceof BedBlock) {
                Direction facing = state.getValue(BedBlock.FACING);
                boolean foot = state.getValue(BedBlock.PART) == BedPart.FOOT;
                BlockPos matePos = entry.getKey().relative(foot ? facing : facing.getOpposite());
                BlockState mate = blocks.get(matePos);
                if (mate == null || mate.getBlock() != state.getBlock()
                        || mate.getValue(BedBlock.PART) == state.getValue(BedBlock.PART)
                        || mate.getValue(BedBlock.FACING) != facing) {
                    invalid.add(entry.getKey());
                }
            }
        }
        blocks.keySet().removeAll(invalid);
    }

    public static Vector3d worldPosition(CameraTransform camera, float x, float y, float depth, float zoom) {
        return worldPosition(camera, x, y, depth, zoom, 1, 36);
    }

    public static float cameraZoomOffset(int captureZoom, float farPlane) {
        float focusDepth = Math.max(4.0f, farPlane - 6.0f);
        return focusDepth * (1.0f - 1.0f / captureZoom);
    }

    public static Vector3d worldPosition(CameraTransform camera, float x, float y, float depth,
                                         float zoom, int captureZoom, float farPlane) {
        // Far moves the complete voxel structure as one rigid object. Stretching each
        // depth independently creates gaps between neighboring captured blocks.
        float adjustedDepth = depth - cameraZoomOffset(captureZoom, farPlane);
        return camera.toWorldSpacePrecise(x, y, zoom == 0.5f ? adjustedDepth + 12.0f : adjustedDepth / zoom);
    }

    public static boolean shouldCarve(float zoom, boolean requested) {
        return requested && zoom != 0.5f;
    }

    private static Rotation rotation(float deltaYaw) {
        float normalized = (deltaYaw % 360 + 360) % 360;
        if (normalized >= 45 && normalized < 135) return Rotation.CLOCKWISE_90;
        if (normalized >= 135 && normalized < 225) return Rotation.CLOCKWISE_180;
        if (normalized >= 225 && normalized < 315) return Rotation.COUNTERCLOCKWISE_90;
        return Rotation.NONE;
    }
}
