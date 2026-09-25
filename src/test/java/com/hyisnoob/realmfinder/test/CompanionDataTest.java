package com.hyisnoob.realmfinder.test;

import com.hyisnoob.realmfinder.core.snapshot.CompanionData;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;

class CompanionDataTest {
    @Test
    void wolfAppearanceKeepsOnlySafeFields() {
        CompoundTag source = new CompoundTag();
        source.putString("variant", "minecraft:woods");
        source.putByte("CollarColor", (byte) 14);
        source.putString("CustomName", "pet");
        source.putString("SaddleItem", "duplicated item");
        source.putString("ArmorItems", "duplicated armor");

        CompoundTag safe = CompanionData.safeAppearance(source, "minecraft:wolf");
        assertEquals("minecraft:woods", safe.getString("variant"));
        assertEquals(14, safe.getByte("CollarColor"));
        assertFalse(safe.contains("CustomName"));
        assertFalse(safe.contains("SaddleItem"));
        assertFalse(safe.contains("ArmorItems"));
    }

    @Test
    void invalidAppearanceDataIsDiscarded() {
        CompoundTag source = new CompoundTag();
        source.putString("variant", "invalid variant");
        source.putInt("CollarColor", 1000);
        source.putInt("Variant", Integer.MAX_VALUE);

        assertTrue(CompanionData.safeAppearance(source, "minecraft:wolf").isEmpty());
        assertTrue(CompanionData.safeAppearance(source, "minecraft:horse").isEmpty());
    }

    @Test
    void storedMobDataKeepsEquipmentInventoryAndVillagerState() {
        CompoundTag source = new CompoundTag();
        UUID owner = UUID.randomUUID();
        source.putUUID("Owner", owner);
        source.putBoolean("Sitting", true);
        source.putString("Inventory", "horse items");
        source.putString("ArmorItems", "zombie armor");
        source.putString("HandItems", "trident");
        source.putString("Offers", "villager trades");
        source.putString("VillagerData", "profession and level");
        source.putFloat("Health", 13.5f);

        CompoundTag safe = CompanionData.safeStoredData(source, "minecraft:wolf");
        assertEquals(owner, safe.getUUID("Owner"));
        assertTrue(safe.getBoolean("Sitting"));
        assertEquals("horse items", safe.getString("Inventory"));
        assertEquals("zombie armor", safe.getString("ArmorItems"));
        assertEquals("trident", safe.getString("HandItems"));
        assertEquals("villager trades", safe.getString("Offers"));
        assertEquals("profession and level", safe.getString("VillagerData"));
        assertEquals(13.5f, safe.getFloat("Health"));
    }

    @Test
    void storedMobDataDropsIdentityLocationAndNestedEntities() {
        CompoundTag source = new CompoundTag();
        source.putUUID("UUID", UUID.randomUUID());
        source.putString("Pos", "old position");
        source.putString("Motion", "old velocity");
        source.putString("Rotation", "old rotation");
        source.putString("Passengers", "nested entities");
        source.putString("Leash", "old leash target");
        source.putString("Brain", "old points of interest");
        source.putString("ArmorItems", "keep this");

        CompoundTag safe = CompanionData.safeStoredData(source, "minecraft:zombie");
        assertFalse(safe.contains("UUID"));
        assertFalse(safe.contains("Pos"));
        assertFalse(safe.contains("Motion"));
        assertFalse(safe.contains("Rotation"));
        assertFalse(safe.contains("Passengers"));
        assertFalse(safe.contains("Leash"));
        assertFalse(safe.contains("Brain"));
        assertEquals("keep this", safe.getString("ArmorItems"));
    }

    @Test
    void balancedRuleRemovesEquipmentButKeepsVillagerProfessionAndPetOwner() {
        CompoundTag source = new CompoundTag();
        source.putUUID("Owner", UUID.randomUUID());
        source.putString("ArmorItems", "armor");
        source.putString("HandItems", "trident");
        source.putString("Inventory", "items");
        source.putString("VillagerData", "farmer");
        CompoundTag filtered = CompanionData.withoutEquipment(source);
        assertFalse(filtered.contains("ArmorItems"));
        assertFalse(filtered.contains("HandItems"));
        assertFalse(filtered.contains("Inventory"));
        assertTrue(filtered.contains("Owner"));
        assertEquals("farmer", filtered.getString("VillagerData"));
    }
}
