package com.hyisnoob.realmfinder.client;

import com.hyisnoob.realmfinder.core.config.RealmFinderConfig;

public final class ServerSettings {
    private static RealmFinderConfig.Settings current = new RealmFinderConfig.Settings();
    private static boolean editable;
    private static boolean received;

    private ServerSettings() {}

    public static RealmFinderConfig.Settings get() { return current; }
    public static boolean editable() { return editable; }
    public static boolean received() { return received; }

    public static void accept(String json, boolean mayEdit) {
        try {
            current = RealmFinderConfig.fromJson(json);
            editable = mayEdit;
            received = true;
        } catch (RuntimeException ignored) {
            received = false;
        }
    }

    public static void reset() {
        received = false;
        editable = false;
    }
}
