package com.hyisnoob.realmfinder.core.engine;

import com.hyisnoob.realmfinder.RealmFinder;
import com.hyisnoob.realmfinder.core.config.RealmFinderConfig;
import com.hyisnoob.realmfinder.core.math.CameraTransform;
import com.hyisnoob.realmfinder.core.math.Frustum;
import com.hyisnoob.realmfinder.core.snapshot.CapturedBlock;
import com.hyisnoob.realmfinder.core.snapshot.CapturedEntity;
import com.hyisnoob.realmfinder.core.snapshot.CompanionData;
import com.hyisnoob.realmfinder.core.snapshot.WorldSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.TorchBlock;
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
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        return capture(level, camera, fov, aspectRatio, nearPlane, farPlane, snapshotId, 1);
    }

    public static WorldSnapshot capture(Level level, CameraTransform camera, float fov, float aspectRatio,
                                         float nearPlane, float farPlane, UUID snapshotId, int captureZoom) {
        WorldSnapshot snapshot = WorldSnapshot.createNew(
                snapshotId, fov, aspectRatio, nearPlane, farPlane, camera.getYaw(), camera.getPitch(), captureZoom
        );

        Frustum frustum = new Frustum(camera, fov, aspectRatio, nearPlane, farPlane);

        Vector3d[] nearCorners = frustum.getCornersAtDistance(nearPlane);
        Vector3d[] farCorners = frustum.getCornersAtDistance(farPlane);

        double minX = camera.getEyeX(), maxX = camera.getEyeX();
        double minY = camera.getEyeY(), maxY = camera.getEyeY();
        double minZ = camera.getEyeZ(), maxZ = camera.getEyeZ();

        for (Vector3d c : nearCorners) {
            minX = Math.min(minX, c.x); maxX = Math.max(maxX, c.x);
            minY = Math.min(minY, c.y); maxY = Math.max(maxY, c.y);
            minZ = Math.min(minZ, c.z); maxZ = Math.max(maxZ, c.z);
        }
        for (Vector3d c : farCorners) {
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
                    if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) return null;
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || state.is(Blocks.BEDROCK)) {
                        continue;
                    }

                    Vector3f camRel = camera.toCameraSpace(x + 0.5, y + 0.5, z + 0.5);

                    int paletteIndex = snapshot.getOrAddPaletteIndex(state);
                    CompoundTag beData = null;
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof SignBlockEntity || (be instanceof ChestBlockEntity
                            && RealmFinderConfig.get().copyContainerContents)) {
                        beData = be.saveWithFullMetadata();
                    }

                    snapshot.getBlocks().add(new CapturedBlock(camRel.x, camRel.y, camRel.z, paletteIndex, beData));
                    if (snapshot.getBlockCount() > RealmFinderConfig.get().maxCapturedBlocks) return null;
                }
            }
        }

        // Entity capture: living mobs only; equipment, inventories and gameplay state are retained.
        AABB entityBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        List<Entity> candidateEntities = RealmFinderConfig.get().allowEntityCapture
                ? level.getEntities((Entity) null, entityBox, entity -> {
            if (!entity.isAlive()) return false;
            if (!(entity instanceof LivingEntity) || entity instanceof Player) return false;
            if (entity instanceof WitherBoss) return false;
            if (entity instanceof EnderDragon) return false;
            return true;
        }) : List.of();

        int entityCount = 0;
        final int MAX_CAPTURED_ENTITIES = RealmFinderConfig.get().maxCapturedEntities;
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
                CompoundTag nbt = CompanionData.capture(entity);
                if (!RealmFinderConfig.get().copyMobEquipment) nbt = CompanionData.withoutEquipment(nbt);
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
    public static boolean stamp(ServerLevel level, WorldSnapshot snapshot, CameraTransform targetCamera, boolean carve, UndoRecord undoRecord) {
        return stamp(level, snapshot, targetCamera, carve, 1.0f, undoRecord);
    }

    public static boolean stamp(ServerLevel level, WorldSnapshot snapshot, CameraTransform targetCamera, boolean carve, float scale, UndoRecord undoRecord) {
        if (undoRecord == null || snapshot == null || (snapshot.getBlocks().isEmpty() && snapshot.getEntities().isEmpty())
                || snapshot.getBlocks().size() > GameplayLimits.MAX_CAPTURE_BLOCKS
                || snapshot.getEntities().size() > GameplayLimits.MAX_CAPTURED_ENTITIES) {
            return false;
        }

        PlacementGeometry.Plan plan = PlacementGeometry.build(snapshot, targetCamera, scale,
                level.getMinBuildHeight(), level.getMaxBuildHeight());
        if (plan == null) return false;
        CameraTransform snappedCamera = plan.camera();
        float snappedYaw = snappedCamera.getYaw();
        Map<BlockPos, BlockState> blocksToPlace = plan.blocks();
        Map<BlockPos, CompoundTag> blockEntitiesToPlace = plan.blockEntities();
        Set<BlockPos> carvePositions = plan.carvePositions();

        for (BlockPos pos : blocksToPlace.keySet()) {
            if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) return false;
            if (level.getBlockEntity(pos) != null) return false;
            if (level.getBlockState(pos).is(Blocks.BEDROCK)) return false;
        }
        if (RealmFinderConfig.get().mayCarve(carve, scale) && !blocksToPlace.isEmpty()) {
            if ((long) blocksToPlace.size() + carvePositions.size() > GameplayLimits.MAX_PLACED_BLOCKS) return false;
            for (BlockPos pos : carvePositions) {
                if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)
                        || level.getBlockEntity(pos) != null) return false;
            }
        }
        for (CapturedEntity captured : snapshot.getEntities()) {
            Vector3d worldPos = PlacementGeometry.worldPosition(snappedCamera,
                    captured.getCamX(), captured.getCamY(), captured.getCamZ(), scale, snapshot.getCaptureZoom(), snapshot.getFarPlane());
            BlockPos pos = BlockPos.containing(worldPos.x, worldPos.y, worldPos.z);
            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)
                    || !level.getWorldBorder().isWithinBounds(pos)) return false;
        }

        try {
        // Clear only empty cells inside the photographed structure's occupied columns.
        if (RealmFinderConfig.get().mayCarve(carve, scale) && !blocksToPlace.isEmpty()) {
            for (BlockPos pos : carvePositions) {
                BlockState existing = level.getBlockState(pos);
                if (!existing.isAir() && !existing.is(Blocks.BEDROCK)) {
                    undoRecord.recordBeforeChange(level, pos);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(),
                            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
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
        int placeFlags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
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
            Vector3d worldPos = PlacementGeometry.worldPosition(snappedCamera,
                    captured.getCamX(), captured.getCamY(), captured.getCamZ(), scale, snapshot.getCaptureZoom(), snapshot.getFarPlane());
            float newYaw = (captured.getYaw() + snappedYaw) % 360.0f;

            ResourceLocation typeId = ResourceLocation.tryParse(captured.getEntityTypeId());
            EntityType<?> type = typeId == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(typeId).orElse(null);
            Entity ent = type == null ? null : type.create(level);

            if (ent instanceof LivingEntity && !(ent instanceof Player)
                    && !(ent instanceof WitherBoss) && !(ent instanceof EnderDragon)) {
                CompoundTag mobData = captured.getEntityNbt();
                if (!RealmFinderConfig.get().copyMobEquipment) mobData = CompanionData.withoutEquipment(mobData);
                CompanionData.restore(ent, mobData);
                ent.moveTo(worldPos.x, worldPos.y, worldPos.z, newYaw, captured.getPitch());
                ent.setUUID(UUID.randomUUID());
                if (level.addFreshEntity(ent) && undoRecord != null) {
                    undoRecord.recordSpawnedEntity(ent.getUUID());
                }
            }
        }
        return undoRecord.hasEffectiveChanges(level);
        } catch (RuntimeException e) {
            RealmFinder.LOGGER.error("Photograph placement failed; restoring changed blocks", e);
            try {
                undoRecord.restore(level, null);
            } catch (RuntimeException rollbackError) {
                RealmFinder.LOGGER.error("Photograph placement rollback also failed", rollbackError);
            }
            return false;
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
            if (be instanceof SignBlockEntity || (be instanceof ChestBlockEntity
                    && RealmFinderConfig.get().copyContainerContents)) {
                CompoundTag beData = blockEntitiesToPlace.get(pos).copy();
                beData.putInt("x", pos.getX());
                beData.putInt("y", pos.getY());
                beData.putInt("z", pos.getZ());
                be.load(beData);
                be.setChanged();
            }
        }
    }

    public static boolean isAttachedOrFragile(BlockState state) {
        Block block = state.getBlock();
        return state.is(Blocks.REDSTONE_WIRE)
                || block instanceof TorchBlock
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

}
