package com.hyisnoob.realmfinder.core.engine;

import com.hyisnoob.realmfinder.RealmFinder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

public class UndoRecord {
    public enum FailureReason { NONE, DIMENSION, CHUNK, CONFLICT }
    private final Map<BlockPos, BlockState> previousStates = new HashMap<>();
    private final Map<BlockPos, BlockState> placedStates = new HashMap<>();
    private final Map<BlockPos, CompoundTag> placedBlockEntities = new HashMap<>();
    private final Map<BlockPos, CompoundTag> previousBlockEntities = new HashMap<>();
    private final List<UUID> spawnedEntityUuids = new ArrayList<>();
    private final ItemStack returnedPhoto;
    private ResourceKey<Level> dimension;
    private FailureReason failureReason = FailureReason.NONE;
    private BlockPos conflictPos;

    public FailureReason failureReason() { return failureReason; }
    public BlockPos conflictPos() { return conflictPos; }

    public UndoRecord(ItemStack photo, boolean consumed) {
        ItemStack singlePhoto = photo.copy();
        singlePhoto.setCount(1);
        this.returnedPhoto = consumed ? singlePhoto : ItemStack.EMPTY;
    }

    public ItemStack photoToReturn() {
        return returnedPhoto.copy();
    }

    public void seal(ServerLevel level) {
        dimension = level.dimension();
        Iterator<Map.Entry<BlockPos, BlockState>> entries = previousStates.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<BlockPos, BlockState> entry = entries.next();
            BlockPos pos = entry.getKey();
            BlockEntity be = level.getBlockEntity(pos);
            CompoundTag after = be == null ? null : be.saveWithFullMetadata();
            CompoundTag before = previousBlockEntities.get(pos);
            if (level.getBlockState(pos) == entry.getValue()
                    && (after == null ? before == null : after.equals(before))) {
                entries.remove();
                previousBlockEntities.remove(pos);
                continue;
            }
            placedStates.put(pos, level.getBlockState(pos));
            if (after != null) placedBlockEntities.put(pos, after);
        }
    }

    public void recordSpawnedEntity(UUID uuid) {
        if (uuid != null) {
            spawnedEntityUuids.add(uuid);
        }
    }

    public void recordBeforeChange(ServerLevel level, BlockPos pos) {
        if (!previousStates.containsKey(pos)) {
            previousStates.put(pos, level.getBlockState(pos));
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                previousBlockEntities.put(pos, be.saveWithFullMetadata());
            }
        }
    }

    public boolean restore(ServerLevel level, ServerPlayer player) {
        failureReason = FailureReason.NONE;
        conflictPos = null;
        if (previousStates.isEmpty() && spawnedEntityUuids.isEmpty()) {
            return false;
        }
        if (dimension != null && !dimension.equals(level.dimension())) {
            failureReason = FailureReason.DIMENSION;
            return false;
        }
        // A player can move far enough away that placed chunks unload. Load each
        // affected chunk once before deciding whether the world changed.
        Set<Long> chunks = new HashSet<>();
        try {
            for (BlockPos pos : previousStates.keySet()) {
                int chunkX = pos.getX() >> 4;
                int chunkZ = pos.getZ() >> 4;
                if (chunks.add(ChunkPos.asLong(chunkX, chunkZ))) level.getChunk(chunkX, chunkZ);
            }
        } catch (RuntimeException e) {
            failureReason = FailureReason.CHUNK;
            return false;
        }
        for (Map.Entry<BlockPos, BlockState> entry : placedStates.entrySet()) {
            BlockState previous = previousStates.get(entry.getKey());
            BlockState current = level.getBlockState(entry.getKey());
            if (!level.hasChunkAt(entry.getKey())
                    || !mayRestore(entry.getValue(), previous, current)) {
                failureReason = FailureReason.CONFLICT;
                conflictPos = entry.getKey();
                RealmFinder.LOGGER.warn("Undo conflict at {}: placed={}, current={}, previous={}",
                        conflictPos, entry.getValue(), current, previous);
                return false;
            }
            if (!sameBlockForUndo(entry.getValue(), current)) continue;
            BlockEntity be = level.getBlockEntity(entry.getKey());
            CompoundTag expected = placedBlockEntities.get(entry.getKey());
            if ((be == null) != (expected == null)) {
                failureReason = FailureReason.CONFLICT;
                conflictPos = entry.getKey();
                RealmFinder.LOGGER.warn("Undo block entity conflict at {}: expected={}, current={}",
                        conflictPos, expected == null ? "none" : expected.getString("id"),
                        be == null ? "none" : be.getType());
                return false;
            }
            if (be != null && !be.saveWithFullMetadata().getString("id")
                    .equals(expected.getString("id"))) {
                failureReason = FailureReason.CONFLICT;
                conflictPos = entry.getKey();
                RealmFinder.LOGGER.warn("Undo block entity type conflict at {}: expected={}, current={}",
                        conflictPos, expected.getString("id"), be.getType());
                return false;
            }
        }

        // 0. Discard all spawned entities from this stamp
        for (UUID uuid : spawnedEntityUuids) {
            Entity entity = level.getEntity(uuid);
            if (entity != null) {
                entity.discard();
            }
        }
        spawnedEntityUuids.clear();

        // Sort positions top-to-bottom (Y descending) so fragile blocks on top are cleared
        // BEFORE supporting blocks below them are replaced (prevents popping off item drops)
        List<BlockPos> sortedPositions = new ArrayList<>(previousStates.keySet());
        sortedPositions.sort((a, b) -> Integer.compare(b.getY(), a.getY()));

        int restoreFlags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
        for (BlockPos pos : sortedPositions) {
            BlockState state = previousStates.get(pos);
            BlockState current = level.getBlockState(pos);
            if (current == state && previousBlockEntities.get(pos) == null
                    && level.getBlockEntity(pos) == null) continue;
            clearContainerBeforeReplacement(level.getBlockEntity(pos));
            level.setBlock(pos, state, restoreFlags);

            if (previousBlockEntities.containsKey(pos)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be != null) {
                    CompoundTag tag = previousBlockEntities.get(pos);
                    be.load(tag);
                    be.setChanged();
                }
            }
        }

        // Notify neighbor updates so restored world state functions normally
        for (BlockPos pos : sortedPositions) {
            level.blockUpdated(pos, previousStates.get(pos).getBlock());
        }

        // Return one consumed photo back to player
        if (player != null && !returnedPhoto.isEmpty()) {
            if (!player.getInventory().add(returnedPhoto.copy())) {
                player.drop(returnedPhoto.copy(), false);
            }
        }

        return true;
    }

    public int getModifiedBlockCount() {
        return previousStates.size();
    }

    public static void clearContainerBeforeReplacement(BlockEntity blockEntity) {
        if (blockEntity instanceof Container container) {
            container.clearContent();
            blockEntity.setChanged();
        }
    }

    public static boolean sameBlockForUndo(BlockState placed, BlockState current) {
        return current.is(placed.getBlock());
    }

    public static boolean mayRestore(BlockState placed, BlockState previous, BlockState current) {
        return sameBlockForUndo(placed, current)
                || current == previous
                || current.isAir()
                || (current.is(Blocks.DIRT) && (placed.is(Blocks.DIRT_PATH)
                || placed.is(Blocks.FARMLAND) || placed.is(Blocks.GRASS_BLOCK)));
    }

    public boolean hasEffectiveChanges(ServerLevel level) {
        if (!spawnedEntityUuids.isEmpty()) return true;
        for (Map.Entry<BlockPos, BlockState> entry : previousStates.entrySet()) {
            BlockPos pos = entry.getKey();
            if (level.getBlockState(pos) != entry.getValue()) return true;
            BlockEntity be = level.getBlockEntity(pos);
            CompoundTag before = previousBlockEntities.get(pos);
            if ((be == null) != (before == null)) return true;
            if (be != null && !be.saveWithFullMetadata().equals(before)) return true;
        }
        return false;
    }
}
