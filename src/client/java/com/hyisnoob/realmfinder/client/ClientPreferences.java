package com.hyisnoob.realmfinder.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hyisnoob.realmfinder.RealmFinder;
import com.hyisnoob.realmfinder.core.config.RealmFinderConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ClientPreferences {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Preferences current = new Preferences();

    private ClientPreferences() {}

    public static Preferences get() { return current; }

    public static void load() {
        Path path = path();
        try {
            if (Files.exists(path)) {
                Preferences parsed = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), Preferences.class);
                current = parsed == null ? new Preferences() : parsed.validated();
            } else {
                current = new Preferences();
                current.maxCameraZoom = RealmFinderConfig.get().maxCameraZoom;
                save(current);
            }
        } catch (Exception e) {
            RealmFinder.LOGGER.warn("Could not load client preferences", e);
            current = new Preferences();
        }
    }

    public static void save(Preferences value) {
        Preferences next = value.validated();
        try {
            Path path = path();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(next), StandardCharsets.UTF_8);
            current = next;
        } catch (Exception e) {
            RealmFinder.LOGGER.warn("Could not save client preferences", e);
        }
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("realmfinder-client.json");
    }

    public static final class Preferences {
        public boolean confirmBeforeUndo = false;
        public int maxCameraZoom = 4;
        public boolean showHudHints = true;

        public Preferences validated() {
            maxCameraZoom = Math.clamp(maxCameraZoom, 1, 6);
            return this;
        }

        public Preferences copy() {
            Preferences result = new Preferences();
            result.confirmBeforeUndo = confirmBeforeUndo;
            result.maxCameraZoom = maxCameraZoom;
            result.showHudHints = showHudHints;
            return result;
        }
    }
}
