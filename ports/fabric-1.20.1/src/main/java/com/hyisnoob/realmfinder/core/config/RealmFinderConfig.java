package com.hyisnoob.realmfinder.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hyisnoob.realmfinder.RealmFinder;
import com.hyisnoob.realmfinder.core.engine.GameplayLimits;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Simple config file shared by client and server installations. Server rules are authoritative. */
public final class RealmFinderConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Settings settings = new Settings();

    private RealmFinderConfig() {}

    public static Settings get() {
        return settings;
    }

    public static String toJson(Settings value) {
        return GSON.toJson(value.validated());
    }

    public static Settings fromJson(String json) {
        if (json == null || json.length() > 2048) throw new IllegalArgumentException("Settings too large");
        Settings parsed = GSON.fromJson(json, Settings.class);
        if (parsed == null) throw new IllegalArgumentException("Missing settings");
        return parsed.validated();
    }

    public static void update(Settings value) {
        Settings next = fromJson(toJson(value));
        Path path = FabricLoader.getInstance().getConfigDir().resolve("realmfinder.json");
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, toJson(next), StandardCharsets.UTF_8);
            settings = next;
        } catch (IOException e) {
            RealmFinder.LOGGER.warn("Could not save RealmFinder settings {}", path, e);
            throw new IllegalStateException("Could not save settings", e);
        }
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("realmfinder.json");
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                Files.writeString(path, GSON.toJson(new Settings()), StandardCharsets.UTF_8);
            }
            Settings parsed = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), Settings.class);
            settings = parsed == null ? new Settings() : parsed.validated();
        } catch (IOException | RuntimeException e) {
            RealmFinder.LOGGER.warn("Could not read RealmFinder config {}; using defaults", path, e);
            settings = new Settings();
        }
    }

    public static final class Settings {
        public int maxCapturedBlocks = GameplayLimits.MAX_CAPTURE_BLOCKS;
        public int maxCapturedEntities = GameplayLimits.MAX_CAPTURED_ENTITIES;
        public int maxUndoSteps = 10;
        public int maxCameraZoom = 4;
        public boolean allowEntityCapture = true;
        public boolean copyContainerContents = true;
        public boolean copyMobEquipment = true;
        public boolean requireEmptyPhotograph = true;
        public boolean useCameraDurability = true;
        public boolean allowPlacementCarving = true;

        public boolean needsBlank(boolean creative) { return !creative && requireEmptyPhotograph; }
        public boolean damagesCamera(boolean creative) { return !creative && useCameraDurability; }
        public boolean mayCarve(boolean requested, float scale) {
            return allowPlacementCarving && requested && scale != 0.5f;
        }

        public Settings validated() {
            maxCapturedBlocks = Math.max(1, Math.min(maxCapturedBlocks, GameplayLimits.MAX_CAPTURE_BLOCKS));
            maxCapturedEntities = Math.max(0, Math.min(maxCapturedEntities, GameplayLimits.MAX_CAPTURED_ENTITIES));
            maxUndoSteps = Math.max(1, Math.min(maxUndoSteps, 20));
            maxCameraZoom = Math.max(1, Math.min(maxCameraZoom, 6));
            return this;
        }
    }
}
