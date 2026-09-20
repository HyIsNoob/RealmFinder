package com.hyisnoob.realmfinder.core.snapshot;

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
        tag.putUUID("SnapshotId", snapshot.getSnapshotId());
        tag.putLong("Timestamp", snapshot.getTimestamp());
        tag.putFloat("Fov", snapshot.getFov());
        tag.putFloat("AspectRatio", snapshot.getAspectRatio());
        tag.putFloat("NearPlane", snapshot.getNearPlane());
        tag.putFloat("FarPlane", snapshot.getFarPlane());
        tag.putFloat("OriginalYaw", snapshot.getOriginalYaw());
        tag.putFloat("OriginalPitch", snapshot.getOriginalPitch());
        tag.putInt("BlockCount", snapshot.getBlockCount());

        // Palette
        ListTag paletteTag = new ListTag();
        for (BlockState state : snapshot.getPalette()) {
            paletteTag.add(NbtUtils.writeBlockState(state));
        }
        tag.put("Palette", paletteTag);

        // Entities
        ListTag entitiesTag = new ListTag();
        for (CapturedEntity entity : snapshot.getEntities()) {
            CompoundTag entTag = new CompoundTag();
            entTag.putFloat("camX", entity.getCamX());
            entTag.putFloat("camY", entity.getCamY());
            entTag.putFloat("camZ", entity.getCamZ());
            entTag.putFloat("yaw", entity.getYaw());
            entTag.putFloat("pitch", entity.getPitch());
            entTag.putString("id", entity.getEntityTypeId());
            entTag.put("data", entity.getEntityNbt());
            entitiesTag.add(entTag);
        }
        tag.put("Entities", entitiesTag);
        tag.putInt("EntityCount", snapshot.getEntityCount());

        // Blocks binary stream
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzos = new GZIPOutputStream(baos);
             DataOutputStream dos = new DataOutputStream(gzos)) {

            dos.writeInt(snapshot.getBlocks().size());
            for (CapturedBlock block : snapshot.getBlocks()) {
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
            e.printStackTrace();
        }

        return tag;
    }

    public static WorldSnapshot fromNbt(CompoundTag tag) {
        if (!tag.hasUUID("SnapshotId")) {
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

        // Palette
        List<BlockState> palette = new ArrayList<>();
        ListTag paletteTag = tag.getList("Palette", Tag.TAG_COMPOUND);
        for (int i = 0; i < paletteTag.size(); i++) {
            CompoundTag stateTag = paletteTag.getCompound(i);
            BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), stateTag);
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
                for (int i = 0; i < size; i++) {
                    float camX = dis.readFloat();
                    float camY = dis.readFloat();
                    float camZ = dis.readFloat();
                    int paletteIndex = dis.readShort();

                    CompoundTag beData = null;
                    if (dis.readBoolean()) {
                        int beLen = dis.readInt();
                        byte[] beBytes = new byte[beLen];
                        dis.readFully(beBytes);
                        ByteArrayInputStream beBais = new ByteArrayInputStream(beBytes);
                        beData = NbtIo.read(new DataInputStream(beBais));
                    }

                    blocks.add(new CapturedBlock(camX, camY, camZ, paletteIndex, beData));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Entities
        List<CapturedEntity> entities = new ArrayList<>();
        if (tag.contains("Entities", Tag.TAG_LIST)) {
            ListTag entitiesTag = tag.getList("Entities", Tag.TAG_COMPOUND);
            for (int i = 0; i < entitiesTag.size(); i++) {
                CompoundTag entTag = entitiesTag.getCompound(i);
                float camX = entTag.getFloat("camX");
                float camY = entTag.getFloat("camY");
                float camZ = entTag.getFloat("camZ");
                float yaw = entTag.getFloat("yaw");
                float pitch = entTag.getFloat("pitch");
                String id = entTag.getString("id");
                CompoundTag data = entTag.getCompound("data");
                entities.add(new CapturedEntity(camX, camY, camZ, yaw, pitch, id, data));
            }
        }

        return new WorldSnapshot(snapshotId, timestamp, fov, aspectRatio,
                nearPlane, farPlane, originalYaw, originalPitch, palette, blocks, entities);
    }
}
