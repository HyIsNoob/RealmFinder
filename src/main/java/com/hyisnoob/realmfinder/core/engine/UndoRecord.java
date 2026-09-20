package com.hyisnoob.realmfinder.core.engine;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UndoRecord {
    private final Map<BlockPos, BlockState> previousStates = new HashMap<>();
    private final Map<BlockPos, CompoundTag> previousBlockEntities = new HashMap<>();
    private final List<UUID> spawnedEntityUuids = new ArrayList<>();
    private final ItemStack returnedPhoto;

    public UndoRecord(ItemStack returnedPhoto) {
        this.returnedPhoto = returnedPhoto.copy();
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
                previousBlockEntities.put(pos, be.saveWithFullMetadata(level.registryAccess()));
            }
        }
    }

    public boolean restore(ServerLevel level, ServerPlayer player) {
        if (previousStates.isEmpty() && spawnedEntityUuids.isEmpty()) {
            return false;
        }

        // 0. Discard all spawned entities from this stamp
        for (UUID uuid : spawnedEntityUuids) {
            Entity entity = level.getEntity(uuid);
            if (entity != null) {
                entity.discard();
            }
        }
        spawnedEntityUuids.clear();

        // 1. Calculate bounding box of all modified blocks
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : previousStates.keySet()) {
            minX = Math.min(minX, pos.getX()); maxX = Math.max(maxX, pos.getX());
            minY = Math.min(minY, pos.getY()); maxY = Math.max(maxY, pos.getY());
            minZ = Math.min(minZ, pos.getZ()); maxZ = Math.max(maxZ, pos.getZ());
        }

        AABB clearBox = new AABB(minX - 2, minY - 2, minZ - 2, maxX + 3, maxY + 3, maxZ + 3);

        // 2. Clean up any loose dropped items (redstone dust, torches, broken blocks) in the area
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, clearBox)) {
            item.discard();
        }

        // 3. Sort positions top-to-bottom (Y descending) so fragile blocks on top are cleared
        // BEFORE supporting blocks below them are replaced (prevents popping off item drops)
        List<BlockPos> sortedPositions = new ArrayList<>(previousStates.keySet());
        sortedPositions.sort((a, b) -> Integer.compare(b.getY(), a.getY()));

        int restoreFlags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        for (BlockPos pos : sortedPositions) {
            BlockState state = previousStates.get(pos);
            level.setBlock(pos, state, restoreFlags);

            if (previousBlockEntities.containsKey(pos)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be != null) {
                    CompoundTag tag = previousBlockEntities.get(pos);
                    be.loadWithComponents(tag, level.registryAccess());
                    be.setChanged();
                }
            }
        }

        // 4. Secondary item drop cleanup in case vanilla dropped anything during block sets
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, clearBox)) {
            item.discard();
        }

        // 5. Notify neighbor updates so restored world state functions normally
        for (BlockPos pos : sortedPositions) {
            level.blockUpdated(pos, previousStates.get(pos).getBlock());
        }

        // 6. Return photo back to player
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
}
