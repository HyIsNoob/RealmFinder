package com.hyisnoob.realmfinder.core.snapshot;

import com.hyisnoob.realmfinder.core.engine.GameplayLimits;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class SnapshotSerializer {

    public static CompoundTag toNbt(WorldSnapshot snapshot) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("FormatVersion", 2);
        tag.putUUID("SnapshotId", snapshot.getSnapshotId());
        tag.putLong("Timestamp", snapshot.getTimestamp());
        tag.putFloat("Fov", snapshot.getFov());
        tag.putFloat("AspectRatio", snapshot.getAspectRatio());
        tag.putFloat("NearPlane", snapshot.getNearPlane());
        tag.putFloat("FarPlane", snapshot.getFarPlane());
        tag.putFloat("OriginalYaw", snapshot.getOriginalYaw());
        tag.putFloat("OriginalPitch", snapshot.getOriginalPitch());
        tag.putInt("CaptureZoom", snapshot.getCaptureZoom());
        tag.putInt("BlockCount", snapshot.getBlockCount());

        // Palette
        ListTag paletteTag = new ListTag();
        for (BlockState state : snapshot.getPalette()) {
            paletteTag.add(NbtUtils.writeBlockState(state));
        }
        tag.put("Palette", paletteTag);

        // Entities
        ListTag entitiesTag = new ListTag();
        int totalEntityBytes = 0;
        for (CapturedEntity entity : snapshot.getEntities()) {
            CompoundTag entTag = new CompoundTag();
            entTag.putFloat("camX", entity.getCamX());
            entTag.putFloat("camY", entity.getCamY());
            entTag.putFloat("camZ", entity.getCamZ());
            entTag.putFloat("yaw", entity.getYaw());
            entTag.putFloat("pitch", entity.getPitch());
            entTag.putString("id", entity.getEntityTypeId());
            CompoundTag entityData = CompanionData.safeStoredData(entity.getEntityNbt(), entity.getEntityTypeId());
            int entityBytes = serializedSize(entityData);
            if (entityBytes < 0 || entityBytes > GameplayLimits.MAX_ENTITY_DATA_BYTES
                    || totalEntityBytes + entityBytes > GameplayLimits.MAX_TOTAL_ENTITY_DATA_BYTES) return null;
            totalEntityBytes += entityBytes;
            entTag.put("data", entityData);
            entitiesTag.add(entTag);
        }
        tag.put("Entities", entitiesTag);
        tag.putInt("EntityCount", snapshot.getEntityCount());

        // Blocks binary stream
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzos = new GZIPOutputStream(baos);
            DataOutputStream dos = new DataOutputStream(gzos)) {

            dos.writeInt(snapshot.getBlocks().size());
            int decodedBytes = 4;
            for (CapturedBlock block : snapshot.getBlocks()) {
                decodedBytes += 15;
                if (decodedBytes > 4 * 1024 * 1024) return null;
                dos.writeFloat(block.getCamX());
                dos.writeFloat(block.getCamY());
                dos.writeFloat(block.getCamZ());
                dos.writeShort(block.getPaletteIndex());

                CompoundTag beData = block.getBlockEntityData();
                if (beData != null) {
                    dos.writeBoolean(true);
                    ByteArrayOutputStream beBaos = new ByteArrayOutputStream();
                    DataOutputStream beDos = new DataOutputStream(beBaos);
                    NbtIo.write(beData, beDos);
                    beDos.flush();
                    byte[] beBytes = beBaos.toByteArray();
                    if (beBytes.length > GameplayLimits.MAX_BLOCK_ENTITY_BYTES) return null;
                    decodedBytes += 4 + beBytes.length;
                    if (decodedBytes > 4 * 1024 * 1024) return null;
                    dos.writeInt(beBytes.length);
                    dos.write(beBytes);
                } else {
                    dos.writeBoolean(false);
                }
            }
            dos.flush();
            gzos.finish();
            tag.putByteArray("CompressedBlocks", baos.toByteArray());
        } catch (IOException e) {
            return null;
        }

        return tag;
    }

    public static WorldSnapshot fromNbt(CompoundTag tag) {
        if (!tag.hasUUID("SnapshotId") || !tag.contains("CompressedBlocks", Tag.TAG_BYTE_ARRAY)
                || tag.getInt("FormatVersion") < 0 || tag.getInt("FormatVersion") > 2) {
            return null;
        }

        UUID snapshotId = tag.getUUID("SnapshotId");
        long timestamp = tag.getLong("Timestamp");
        float fov = tag.getFloat("Fov");
        float aspectRatio = tag.getFloat("AspectRatio");
        float nearPlane = tag.getFloat("NearPlane");
        float farPlane = tag.getFloat("FarPlane");
        float originalYaw = tag.getFloat("OriginalYaw");
        float originalPitch = tag.getFloat("OriginalPitch");
        int captureZoom = tag.getInt("FormatVersion") >= 2 ? tag.getInt("CaptureZoom") : 1;
        if (!Float.isFinite(fov) || fov < 1 || fov > 100
                || !Float.isFinite(aspectRatio) || aspectRatio != 1.0f
                || !Float.isFinite(nearPlane) || nearPlane != 1.0f
                || !Float.isFinite(farPlane) || farPlane < 1 || farPlane > 36
                || !Float.isFinite(originalYaw) || !Float.isFinite(originalPitch)
                || captureZoom < 1 || captureZoom > 6
                || tag.getByteArray("CompressedBlocks").length > GameplayLimits.MAX_COMPRESSED_BLOCK_BYTES) {
            return null;
        }

        // Palette
        List<BlockState> palette = new ArrayList<>();
        ListTag paletteTag = tag.getList("Palette", Tag.TAG_COMPOUND);
        if (paletteTag.size() > 32767) return null;
        for (int i = 0; i < paletteTag.size(); i++) {
            CompoundTag stateTag = paletteTag.getCompound(i);
            BlockState state;
            try {
                state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), stateTag);
            } catch (RuntimeException e) {
                return null;
            }
            if (state == null) {
                state = Blocks.AIR.defaultBlockState();
            }
            palette.add(state);
        }

        // Blocks binary stream
        List<CapturedBlock> blocks = new ArrayList<>();
        if (tag.contains("CompressedBlocks", Tag.TAG_BYTE_ARRAY)) {
            byte[] bytes = tag.getByteArray("CompressedBlocks");
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                 GZIPInputStream gzis = new GZIPInputStream(bais);
                 DataInputStream dis = new DataInputStream(gzis)) {

                int size = dis.readInt();
                if (size < 0 || size > GameplayLimits.MAX_CAPTURE_BLOCKS) return null;
                int decodedBytes = 4;
                for (int i = 0; i < size; i++) {
                    decodedBytes += 15;
                    if (decodedBytes > 4 * 1024 * 1024) return null;
                    float camX = dis.readFloat();
                    float camY = dis.readFloat();
                    float camZ = dis.readFloat();
                    int paletteIndex = dis.readShort();
                    if (!Float.isFinite(camX) || !Float.isFinite(camY) || !Float.isFinite(camZ)
                            || Math.abs(camX) > 128 || Math.abs(camY) > 128 || Math.abs(camZ) > 128
                            || paletteIndex < 0 || paletteIndex >= palette.size()) return null;

                    CompoundTag beData = null;
                    if (dis.readBoolean()) {
                        int beLen = dis.readInt();
                        if (beLen < 0 || beLen > GameplayLimits.MAX_BLOCK_ENTITY_BYTES) return null;
                        decodedBytes += 4 + beLen;
                        if (decodedBytes > 4 * 1024 * 1024) return null;
                        byte[] beBytes = new byte[beLen];
                        dis.readFully(beBytes);
                        ByteArrayInputStream beBais = new ByteArrayInputStream(beBytes);
                        beData = NbtIo.read(new DataInputStream(beBais));
                    }

                    blocks.add(new CapturedBlock(camX, camY, camZ, paletteIndex, beData));
                }
            } catch (IOException e) {
                return null;
            } catch (RuntimeException e) {
                return null;
            }
        }

        // Entities
        List<CapturedEntity> entities = new ArrayList<>();
        int totalEntityBytes = 0;
        if (tag.contains("Entities", Tag.TAG_LIST)) {
            ListTag entitiesTag = tag.getList("Entities", Tag.TAG_COMPOUND);
            if (entitiesTag.size() > GameplayLimits.MAX_CAPTURED_ENTITIES) return null;
            for (int i = 0; i < entitiesTag.size(); i++) {
                CompoundTag entTag = entitiesTag.getCompound(i);
                float camX = entTag.getFloat("camX");
                float camY = entTag.getFloat("camY");
                float camZ = entTag.getFloat("camZ");
                float yaw = entTag.getFloat("yaw");
                float pitch = entTag.getFloat("pitch");
                String id = entTag.getString("id");
                if (!Float.isFinite(camX) || !Float.isFinite(camY) || !Float.isFinite(camZ)
                        || Math.abs(camX) > 128 || Math.abs(camY) > 128 || Math.abs(camZ) > 128
                        || !Float.isFinite(yaw) || !Float.isFinite(pitch) || id.length() > 128) return null;
                CompoundTag data = CompanionData.safeStoredData(entTag.getCompound("data"), id);
                int entityBytes = serializedSize(data);
                if (entityBytes < 0 || entityBytes > GameplayLimits.MAX_ENTITY_DATA_BYTES
                        || totalEntityBytes + entityBytes > GameplayLimits.MAX_TOTAL_ENTITY_DATA_BYTES) return null;
                totalEntityBytes += entityBytes;
                entities.add(new CapturedEntity(camX, camY, camZ, yaw, pitch, id, data));
            }
        }

        return new WorldSnapshot(snapshotId, timestamp, fov, aspectRatio,
                nearPlane, farPlane, originalYaw, originalPitch, captureZoom, palette, blocks, entities);
    }

    private static int serializedSize(CompoundTag tag) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(bytes)) {
            NbtIo.write(tag, out);
            out.flush();
            return bytes.size();
        } catch (IOException | RuntimeException e) {
            return -1;
        }
    }
}
