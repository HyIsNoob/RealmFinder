package com.hyisnoob.realmfinder.core.snapshot;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.Set;

/** Captures mob state while removing identity, position and nested entity links. */
public final class CompanionData {
    private static final Set<String> UNSAFE_KEYS = Set.of(
            "UUID", "UUIDMost", "UUIDLeast",
            "Pos", "Motion", "Rotation", "Dimension",
            "Passengers", "Leash", "RootVehicle",
            "Brain", "AngryAt", "HurtBy", "LoveCause"
    );
    private static final Set<String> EQUIPMENT_KEYS = Set.of(
            "ArmorItems", "HandItems", "ArmorDropChances", "HandDropChances",
            "Inventory", "Items", "SaddleItem", "DecorItem", "body_armor_item", "equipment"
    );

    private CompanionData() {}

    public static CompoundTag capture(Entity entity) {
        String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        return safeStoredData(entity.saveWithoutId(new CompoundTag()), typeId);
    }

    public static void restore(Entity entity, CompoundTag data) {
        if (data == null || data.isEmpty()) return;
        String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        CompoundTag safe = safeStoredData(data, typeId);
        CompoundTag fresh = entity.saveWithoutId(new CompoundTag());
        for (String key : safe.getAllKeys()) fresh.put(key, safe.get(key).copy());
        entity.load(fresh);
        // Photographs from the companion-only alpha used a plain Name field.
        if (!data.contains("CustomName") && data.contains("Name", Tag.TAG_STRING)) {
            String legacyName = data.getString("Name");
            if (!legacyName.isEmpty()) {
                entity.setCustomName(Component.literal(legacyName.substring(0, Math.min(legacyName.length(), 128))));
                entity.setCustomNameVisible(data.getBoolean("NameVisible"));
            }
        }
    }

    public static CompoundTag safeAppearance(CompoundTag source, String typeId) {
        CompoundTag result = new CompoundTag();
        if (source == null) return result;
        if ("minecraft:wolf".equals(typeId) || "minecraft:cat".equals(typeId)) {
            String variant = source.getString("variant");
            if (source.contains("variant", Tag.TAG_STRING) && variant.length() <= 64
                    && variant.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) result.putString("variant", variant);
            if (source.contains("CollarColor", Tag.TAG_BYTE)) {
                int color = source.getByte("CollarColor");
                if (color >= 0 && color <= 15) result.putByte("CollarColor", (byte) color);
            }
        } else if ("minecraft:parrot".equals(typeId)) {
            if (source.contains("Variant", Tag.TAG_INT)) {
                int variant = source.getInt("Variant");
                if (variant >= 0 && variant <= 4) result.putInt("Variant", variant);
            }
        } else if ("minecraft:horse".equals(typeId)) {
            if (source.contains("Variant", Tag.TAG_INT)) {
                int variant = source.getInt("Variant");
                if (variant >= 0 && variant <= 0x504) result.putInt("Variant", variant);
            }
        }
        return result;
    }

    public static CompoundTag safeStoredData(CompoundTag source, String typeId) {
        CompoundTag result = new CompoundTag();
        if (source == null) return result;
        for (String key : source.getAllKeys()) {
            if (!UNSAFE_KEYS.contains(key) && source.get(key) != null) {
                result.put(key, source.get(key).copy());
            }
        }
        return result;
    }

    public static CompoundTag withoutEquipment(CompoundTag source) {
        CompoundTag result = source == null ? new CompoundTag() : source.copy();
        for (String key : EQUIPMENT_KEYS) result.remove(key);
        return result;
    }
}
