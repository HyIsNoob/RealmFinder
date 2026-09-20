package com.hyisnoob.realmfinder.core.engine;

import com.hyisnoob.realmfinder.core.math.CameraTransform;
import com.hyisnoob.realmfinder.core.math.Frustum;
import com.hyisnoob.realmfinder.core.snapshot.CapturedBlock;
import com.hyisnoob.realmfinder.core.snapshot.CapturedEntity;
import com.hyisnoob.realmfinder.core.snapshot.WorldSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.BaseTorchBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ViewfinderEngine {

    public static final float DEFAULT_FOV = 70.0f;
    public static final float DEFAULT_ASPECT_RATIO = 1.0f;
    public static final float DEFAULT_NEAR_PLANE = 1.0f;
    public static final float DEFAULT_FAR_PLANE = 36.0f;

    public static WorldSnapshot capture(Level level, CameraTransform camera, UUID snapshotId) {
        return capture(level, camera, DEFAULT_FOV, DEFAULT_ASPECT_RATIO, DEFAULT_NEAR_PLANE, DEFAULT_FAR_PLANE, snapshotId);
    }

    public static WorldSnapshot capture(Level level, CameraTransform camera, float fov, float aspectRatio,
                                         float nearPlane, float farPlane, UUID snapshotId) {
        WorldSnapshot snapshot = WorldSnapshot.createNew(
                snapshotId, fov, aspectRatio, nearPlane, farPlane, camera.getYaw(), camera.getPitch()
        );

        Frustum frustum = new Frustum(camera, fov, aspectRatio, nearPlane, farPlane);

        Vector3f[] nearCorners = frustum.getCornersAtDistance(nearPlane);
        Vector3f[] farCorners = frustum.getCornersAtDistance(farPlane);

        double minX = camera.getEyeX(), maxX = camera.getEyeX();
        double minY = camera.getEyeY(), maxY = camera.getEyeY();
        double minZ = camera.getEyeZ(), maxZ = camera.getEyeZ();

        for (Vector3f c : nearCorners) {
            minX = Math.min(minX, c.x); maxX = Math.max(maxX, c.x);
            minY = Math.min(minY, c.y); maxY = Math.max(maxY, c.y);
            minZ = Math.min(minZ, c.z); maxZ = Math.max(maxZ, c.z);
        }
        for (Vector3f c : farCorners) {
            minX = Math.min(minX, c.x); maxX = Math.max(maxX, c.x);
            minY = Math.min(minY, c.y); maxY = Math.max(maxY, c.y);
            minZ = Math.min(minZ, c.z); maxZ = Math.max(maxZ, c.z);
        }

        int bMinX = (int) Math.floor(minX);
        int bMaxX = (int) Math.ceil(maxX);
        int bMinY = Math.max(level.getMinBuildHeight(), (int) Math.floor(minY));
        int bMaxY = Math.min(level.getMaxBuildHeight() - 1, (int) Math.ceil(maxY));
        int bMinZ = (int) Math.floor(minZ);
        int bMaxZ = (int) Math.ceil(maxZ);

        for (int y = bMinY; y <= bMaxY; y++) {
            for (int x = bMinX; x <= bMaxX; x++) {
                for (int z = bMinZ; z <= bMaxZ; z++) {
                    if (!frustum.containsBlock(x, y, z)) {
                        continue;
                    }

                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || state.is(Blocks.BEDROCK)) {
                        continue;
                    }

                    Vector3f camRel = camera.toCameraSpace(x + 0.5, y + 0.5, z + 0.5);

                    int paletteIndex = snapshot.getOrAddPaletteIndex(state);
                    CompoundTag beData = null;
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be != null) {
                        beData = be.saveWithFullMetadata(level.registryAccess());
                    }

                    snapshot.getBlocks().add(new CapturedBlock(camRel.x, camRel.y, camRel.z, paletteIndex, beData));
                }
            }
        }

        // Entity Capture: Living entities, armor stands, item frames, minecarts, boats
        AABB entityBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        List<Entity> candidateEntities = level.getEntities((Entity) null, entityBox, entity -> {
            if (!entity.isAlive()) return false;
            if (entity instanceof Player) return false;
            if (entity instanceof WitherBoss) return false;
            if (entity instanceof EnderDragon) return false;
            return true;
        });

        int entityCount = 0;
        final int MAX_CAPTURED_ENTITIES = 16;
        for (Entity entity : candidateEntities) {
            if (entityCount >= MAX_CAPTURED_ENTITIES) break;
            double ex = entity.getX();
            double ey = entity.getY() + entity.getBbHeight() * 0.5;
            double ez = entity.getZ();
            if (frustum.containsPoint(ex, ey, ez)) {
                Vector3f camRel = camera.toCameraSpace(entity.getX(), entity.getY(), entity.getZ());
                float relYaw = entity.getYRot() - camera.getYaw();
                float relPitch = entity.getXRot();
                String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
                CompoundTag nbt = new CompoundTag();
                entity.saveWithoutId(nbt);
                snapshot.getEntities().add(new CapturedEntity(camRel.x, camRel.y, camRel.z, relYaw, relPitch, typeId, nbt));
                entityCount++;
            }
        }

        return snapshot;
    }

    /**
     * Stretches and stamps the snapshot into the world with clean 90-degree alignment
     * and safe non-destructive bounding-box carving.
     */
    public static void stamp(ServerLevel level, WorldSnapshot snapshot, CameraTransform targetCamera, boolean carve, UndoRecord undoRecord) {
        stamp(level, snapshot, targetCamera, carve, 1.0f, undoRecord);
    }

    public static void stamp(ServerLevel level, WorldSnapshot snapshot, CameraTransform targetCamera, boolean carve, float scale, UndoRecord undoRecord) {
        if (snapshot.getBlocks().isEmpty() && snapshot.getEntities().isEmpty()) {
            return;
        }

        // Clamp scale to supported discrete factors
        if (scale < 0.75f) {
            scale = 0.5f;
        } else if (scale >= 1.5f && scale < 2.5f) {
            scale = 2.0f;
        } else if (scale >= 2.5f) {
            scale = 3.0f;
        } else {
            scale = 1.0f;
        }

        // Snap target camera angles to clean 90-degree increments for pristine voxel alignment
        float snappedYaw = Math.round(targetCamera.getYaw() / 90.0f) * 90.0f;
        float snappedPitch = Math.round(targetCamera.getPitch() / 45.0f) * 45.0f;
        // Keep pitch clean: if close to horizontal, snap to 0
        if (Math.abs(snappedPitch) < 25.0f) {
            snappedPitch = 0.0f;
        }

        CameraTransform snappedCamera = new CameraTransform(
                targetCamera.getEyeX(), targetCamera.getEyeY(), targetCamera.getEyeZ(),
                snappedYaw, snappedPitch
        );

        float deltaYaw = snappedYaw - snapshot.getOriginalYaw();
        Rotation rotation = calculateRotation(deltaYaw);

        // Map of target block pos -> block to place
        Map<BlockPos, BlockState> blocksToPlace = new HashMap<>();
        Map<BlockPos, CompoundTag> blockEntitiesToPlace = new HashMap<>();

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        int expansion = (int) Math.round(scale);

        for (CapturedBlock block : snapshot.getBlocks()) {
            BlockState originalState = snapshot.getBlockState(block.getPaletteIndex());
            if (originalState == null || originalState.isAir()) continue;

            BlockState rotatedState = originalState.rotate(rotation);
            boolean fragile = isAttachedOrFragile(rotatedState);

            Vector3f worldPos = snappedCamera.toWorldSpace(
                    block.getCamX() * scale,
                    block.getCamY() * scale,
                    block.getCamZ() * scale
            );
            BlockPos basePos = BlockPos.containing(worldPos.x, worldPos.y, worldPos.z);

            if (expansion >= 2) {
                // Voxel dilation for Giant / Colossal scale
                for (int dx = 0; dx < expansion; dx++) {
                    for (int dy = 0; dy < expansion; dy++) {
                        for (int dz = 0; dz < expansion; dz++) {
                            // Fragile blocks placed only at base anchor to prevent duplication conflicts
                            if (fragile && (dx != 0 || dy != 0 || dz != 0)) {
                                continue;
                            }
                            BlockPos pos = basePos.offset(dx, dy, dz);
                            if (level.isOutsideBuildHeight(pos)) continue;

                            blocksToPlace.put(pos, rotatedState);
                            if (block.getBlockEntityData() != null && dx == 0 && dy == 0 && dz == 0) {
                                blockEntitiesToPlace.put(pos, block.getBlockEntityData());
                            }

                            minX = Math.min(minX, pos.getX()); maxX = Math.max(maxX, pos.getX());
                            minY = Math.min(minY, pos.getY()); maxY = Math.max(maxY, pos.getY());
                            minZ = Math.min(minZ, pos.getZ()); maxZ = Math.max(maxZ, pos.getZ());
                        }
                    }
                }
            } else {
                // Scale 1.0f or miniature 0.5f
                if (level.isOutsideBuildHeight(basePos)) continue;

                blocksToPlace.put(basePos, rotatedState);
                if (block.getBlockEntityData() != null) {
                    blockEntitiesToPlace.put(basePos, block.getBlockEntityData());
                }

                minX = Math.min(minX, basePos.getX()); maxX = Math.max(maxX, basePos.getX());
                minY = Math.min(minY, basePos.getY()); maxY = Math.max(maxY, basePos.getY());
                minZ = Math.min(minZ, basePos.getZ()); maxZ = Math.max(maxZ, basePos.getZ());
            }
        }

        // Safe Carve Phase: ONLY carve strictly inside the bounding box above the ground plane!
        if (carve && !blocksToPlace.isEmpty() && maxY > minY) {
            Frustum targetFrustum = new Frustum(
                    snappedCamera, snapshot.getFov(), snapshot.getAspectRatio(),
                    snapshot.getNearPlane(), snapshot.getFarPlane() * scale
            );

            for (int y = minY + 1; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (!blocksToPlace.containsKey(pos) && targetFrustum.containsPoint(x + 0.5, y + 0.5, z + 0.5)) {
                            BlockState existing = level.getBlockState(pos);
                            if (!existing.isAir() && !existing.is(Blocks.BEDROCK)) {
                                if (undoRecord != null) {
                                    undoRecord.recordBeforeChange(level, pos);
                                }
                                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                            }
                        }
                    }
                }
            }
        }

        // Placement Phase: 2-Pass Placement (Foundations First, Attached/Redstone Second)
        List<Map.Entry<BlockPos, BlockState>> pass1 = new ArrayList<>();
        List<Map.Entry<BlockPos, BlockState>> pass2 = new ArrayList<>();

        for (Map.Entry<BlockPos, BlockState> entry : blocksToPlace.entrySet()) {
            if (isAttachedOrFragile(entry.getValue())) {
                pass2.add(entry);
            } else {
                pass1.add(entry);
            }
        }

        // Sort both passes bottom-to-top (Y ascending) so supports are placed before blocks above them
        pass1.sort(Comparator.comparingInt(e -> e.getKey().getY()));
        pass2.sort(Comparator.comparingInt(e -> e.getKey().getY()));

        // Pass 1: Solid structural foundations (suppress early neighbor drops)
        int placeFlags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        for (Map.Entry<BlockPos, BlockState> entry : pass1) {
            placeSingleBlock(level, entry.getKey(), entry.getValue(), blockEntitiesToPlace, undoRecord, placeFlags);
        }

        // Pass 2: Attached & Redstone blocks (supporting blocks now 100% exist underneath!)
        for (Map.Entry<BlockPos, BlockState> entry : pass2) {
            placeSingleBlock(level, entry.getKey(), entry.getValue(), blockEntitiesToPlace, undoRecord, placeFlags);
        }

        // Pass 3: Notify neighbor updates so redstone wires link, repeaters sync, and shapes connect cleanly!
        for (Map.Entry<BlockPos, BlockState> entry : pass2) {
            BlockPos pos = entry.getKey();
            level.updateNeighborsAt(pos, entry.getValue().getBlock());
        }

        // Pass 4: Entity Materialization (Mobs, Animals, Armor Stands, etc.)
        for (CapturedEntity captured : snapshot.getEntities()) {
            Vector3f worldPos = snappedCamera.toWorldSpace(
                    captured.getCamX() * scale,
                    captured.getCamY() * scale,
                    captured.getCamZ() * scale
            );
            float newYaw = (captured.getYaw() + snappedYaw) % 360.0f;

            CompoundTag nbt = captured.getEntityNbt();
            nbt.remove("UUID");
            nbt.putString("id", captured.getEntityTypeId());

            Entity ent = EntityType.loadEntityRecursive(nbt, level, e -> {
                e.moveTo(worldPos.x, worldPos.y, worldPos.z, newYaw, captured.getPitch());
                return e;
            });

            if (ent != null) {
                ent.setUUID(UUID.randomUUID());
                level.addFreshEntity(ent);
                if (undoRecord != null) {
                    undoRecord.recordSpawnedEntity(ent.getUUID());
                }
            }
        }
    }

    private static void placeSingleBlock(ServerLevel level, BlockPos pos, BlockState state,
                                         Map<BlockPos, CompoundTag> blockEntitiesToPlace,
                                         UndoRecord undoRecord, int flags) {
        BlockState existing = level.getBlockState(pos);
        if (existing.is(Blocks.BEDROCK)) return;

        if (undoRecord != null) {
            undoRecord.recordBeforeChange(level, pos);
        }

        level.setBlock(pos, state, flags);

        if (blockEntitiesToPlace.containsKey(pos)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                CompoundTag beData = blockEntitiesToPlace.get(pos).copy();
                beData.putInt("x", pos.getX());
                beData.putInt("y", pos.getY());
                beData.putInt("z", pos.getZ());
                be.loadWithComponents(beData, level.registryAccess());
                be.setChanged();
            }
        }
    }

    public static boolean isAttachedOrFragile(BlockState state) {
        Block block = state.getBlock();
        return state.is(Blocks.REDSTONE_WIRE)
                || block instanceof BaseTorchBlock
                || block instanceof FaceAttachedHorizontalDirectionalBlock
                || block instanceof DiodeBlock
                || block instanceof BaseRailBlock
                || block instanceof CarpetBlock
                || block instanceof BushBlock
                || block instanceof LadderBlock
                || block instanceof DoorBlock
                || block instanceof TrapDoorBlock
                || block instanceof SignBlock
                || block instanceof PressurePlateBlock
                || block instanceof LanternBlock
                || block instanceof BellBlock
                || block instanceof TripWireBlock
                || block instanceof TripWireHookBlock;
    }

    private static Rotation calculateRotation(float deltaYaw) {
        float normalized = (deltaYaw % 360 + 360) % 360;
        if (normalized >= 45 && normalized < 135) {
            return Rotation.CLOCKWISE_90;
        } else if (normalized >= 135 && normalized < 225) {
            return Rotation.CLOCKWISE_180;
        } else if (normalized >= 225 && normalized < 315) {
            return Rotation.COUNTERCLOCKWISE_90;
        }
        return Rotation.NONE;
    }
}
