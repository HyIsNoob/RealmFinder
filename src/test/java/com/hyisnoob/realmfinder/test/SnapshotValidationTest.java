package com.hyisnoob.realmfinder.test;

import com.hyisnoob.realmfinder.core.snapshot.SnapshotSerializer;
import com.hyisnoob.realmfinder.core.snapshot.WorldSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SnapshotValidationTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void rejectsNegativeBlockCount() throws IOException {
        CompoundTag tag = validHeader();
        tag.putByteArray("CompressedBlocks", compressed(out -> out.writeInt(-1)));
        assertNull(SnapshotSerializer.fromNbt(tag));
    }

    @Test
    void rejectsOversizedBlockEntityAllocation() throws IOException {
        CompoundTag tag = validHeader();
        tag.putByteArray("CompressedBlocks", compressed(out -> {
            out.writeInt(1);
            out.writeFloat(0);
            out.writeFloat(0);
            out.writeFloat(5);
            out.writeShort(0);
            out.writeBoolean(true);
            out.writeInt(Integer.MAX_VALUE);
        }));
        assertNull(SnapshotSerializer.fromNbt(tag));
    }

    @Test
    void rejectsUnknownFutureSnapshotFormat() throws IOException {
        CompoundTag tag = validHeader();
        tag.putInt("FormatVersion", 999);
        tag.putByteArray("CompressedBlocks", compressed(out -> out.writeInt(0)));
        assertNull(SnapshotSerializer.fromNbt(tag));
    }

    @Test
    void roundTripRetainsCaptureZoomAndChestData() {
        WorldSnapshot snapshot = WorldSnapshot.createNew(UUID.randomUUID(), 20, 1, 1, 16, 0, 0, 2);
        snapshot.getOrAddPaletteIndex(net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
        CompoundTag chestData = new CompoundTag();
        chestData.putString("id", "minecraft:chest");
        CompoundTag item = new CompoundTag();
        item.putByte("Slot", (byte) 0);
        item.putString("id", "minecraft:diamond");
        item.putInt("count", 8);
        ListTag items = new ListTag();
        items.add(item);
        chestData.put("Items", items);
        snapshot.getBlocks().add(new com.hyisnoob.realmfinder.core.snapshot.CapturedBlock(0, 0, 10, 0, chestData));

        WorldSnapshot restored = SnapshotSerializer.fromNbt(SnapshotSerializer.toNbt(snapshot));
        assertEquals(2, restored.getCaptureZoom());
        assertEquals(8, restored.getBlocks().get(0).getBlockEntityData()
                .getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(0).getInt("count"));
    }

    @Test
    void legacyPhotographDefaultsToUnzoomedPlacement() throws IOException {
        CompoundTag tag = validHeader();
        tag.putInt("FormatVersion", 1);
        tag.putByteArray("CompressedBlocks", compressed(out -> out.writeInt(0)));
        assertEquals(1, SnapshotSerializer.fromNbt(tag).getCaptureZoom());
    }

    @Test
    void rejectsInvalidCaptureZoom() throws IOException {
        CompoundTag tag = validHeader();
        tag.putInt("FormatVersion", 2);
        tag.putInt("CaptureZoom", 100);
        tag.putByteArray("CompressedBlocks", compressed(out -> out.writeInt(0)));
        assertNull(SnapshotSerializer.fromNbt(tag));
    }

    @Test
    void rejectsOversizedMobState() {
        WorldSnapshot snapshot = WorldSnapshot.createNew(UUID.randomUUID(), 40, 1, 1, 36, 0, 0);
        CompoundTag data = new CompoundTag();
        data.putByteArray("ArmorItems", new byte[140_000]);
        snapshot.getEntities().add(new com.hyisnoob.realmfinder.core.snapshot.CapturedEntity(
                0, 0, 5, 0, 0, "minecraft:zombie", data));
        assertNull(SnapshotSerializer.toNbt(snapshot));
    }

    @Test
    void rejectsChestDataAbovePerBlockLimitBeforeGivingPhotograph() {
        WorldSnapshot snapshot = WorldSnapshot.createNew(UUID.randomUUID(), 40, 1, 1, 36, 0, 0);
        snapshot.getOrAddPaletteIndex(net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
        CompoundTag chestData = new CompoundTag();
        chestData.putString("id", "minecraft:chest");
        chestData.putByteArray("OversizedContents", new byte[70_000]);
        snapshot.getBlocks().add(new com.hyisnoob.realmfinder.core.snapshot.CapturedBlock(0, 0, 5, 0, chestData));
        assertNull(SnapshotSerializer.toNbt(snapshot));
    }

    @Test
    void retainsMobOwnerEquipmentAndVillagerDataWhileDiscardingIdentity() throws IOException {
        CompoundTag tag = validHeader();
        tag.putByteArray("CompressedBlocks", compressed(out -> out.writeInt(0)));
        UUID owner = UUID.randomUUID();
        CompoundTag data = new CompoundTag();
        data.putUUID("Owner", owner);
        data.putString("Name", "Buddy");
        data.putString("ArmorItems", "forbidden");
        data.putString("HandItems", "trident");
        data.putString("Offers", "trades");
        data.putUUID("UUID", UUID.randomUUID());
        CompoundTag entity = new CompoundTag();
        entity.putString("id", "minecraft:wolf");
        entity.put("data", data);
        ListTag entities = new ListTag();
        entities.add(entity);
        tag.put("Entities", entities);

        var snapshot = SnapshotSerializer.fromNbt(tag);
        var restored = snapshot.getEntities().get(0).getEntityNbt();
        assertEquals(owner, restored.getUUID("Owner"));
        assertEquals("Buddy", restored.getString("Name"));
        assertEquals("forbidden", restored.getString("ArmorItems"));
        assertEquals("trident", restored.getString("HandItems"));
        assertEquals("trades", restored.getString("Offers"));
        assertFalse(restored.contains("UUID"));
    }

    private static CompoundTag validHeader() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("SnapshotId", UUID.randomUUID());
        tag.putFloat("Fov", 40);
        tag.putFloat("AspectRatio", 1);
        tag.putFloat("NearPlane", 1);
        tag.putFloat("FarPlane", 36);
        return tag;
    }

    private static byte[] compressed(Writer writer) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bytes))) {
            writer.write(out);
        }
        return bytes.toByteArray();
    }

    private interface Writer {
        void write(DataOutputStream out) throws IOException;
    }
}
