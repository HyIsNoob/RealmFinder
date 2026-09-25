package com.hyisnoob.realmfinder.client.gui;

import com.hyisnoob.realmfinder.client.ClientPreferences;
import com.hyisnoob.realmfinder.client.ServerSettings;
import com.hyisnoob.realmfinder.common.network.SettingsRequestPayload;
import com.hyisnoob.realmfinder.common.network.SettingsUpdatePayload;
import com.hyisnoob.realmfinder.core.config.RealmFinderConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public final class RealmFinderSettingsScreen extends Screen {
    private RealmFinderConfig.Settings rules;
    private ClientPreferences.Preferences personal;
    private final List<Button> ruleButtons = new ArrayList<>();
    private int panelX;
    private int panelY;
    private int panelWidth;
    private boolean renderingWidgets;

    public RealmFinderSettingsScreen() {
        super(Component.translatable("screen.realmfinder.settings"));
        rules = RealmFinderConfig.fromJson(RealmFinderConfig.toJson(ServerSettings.get()));
        personal = ClientPreferences.get().copy();
        ClientPlayNetworking.send(new SettingsRequestPayload());
    }

    public void applyServerSettings() {
        rules = RealmFinderConfig.fromJson(RealmFinderConfig.toJson(ServerSettings.get()));
        if (minecraft != null) rebuildWidgets();
    }

    @Override
    protected void init() {
        panelWidth = Math.min(430, width - 20);
        panelX = (width - panelWidth) / 2;
        panelY = Math.max(4, (height - 240) / 2);
        int columnWidth = (panelWidth - 30) / 2;
        int left = panelX + 10;
        int right = panelX + 20 + columnWidth;
        int row = panelY + 36;
        ruleButtons.clear();

        ruleToggle(left, row, columnWidth, "copy_chest", () -> rules.copyContainerContents, v -> rules.copyContainerContents = v);
        ruleToggle(left, row + 22, columnWidth, "copy_mob", () -> rules.copyMobEquipment, v -> rules.copyMobEquipment = v);
        ruleToggle(left, row + 44, columnWidth, "entities", () -> rules.allowEntityCapture, v -> rules.allowEntityCapture = v);
        ruleToggle(left, row + 66, columnWidth, "blank", () -> rules.requireEmptyPhotograph, v -> rules.requireEmptyPhotograph = v);
        ruleToggle(left, row + 88, columnWidth, "durability", () -> rules.useCameraDurability, v -> rules.useCameraDurability = v);
        ruleToggle(right, row, columnWidth, "carving", () -> rules.allowPlacementCarving, v -> rules.allowPlacementCarving = v);
        ruleNumber(right, row + 22, columnWidth, "blocks", () -> rules.maxCapturedBlocks, v -> rules.maxCapturedBlocks = v, 1, 16384, 1024);
        ruleNumber(right, row + 44, columnWidth, "mobs", () -> rules.maxCapturedEntities, v -> rules.maxCapturedEntities = v, 0, 16, 1);
        ruleNumber(right, row + 66, columnWidth, "history", () -> rules.maxUndoSteps, v -> rules.maxUndoSteps = v, 1, 20, 1);

        personalToggle(left, row + 119, columnWidth, "confirm_undo", () -> personal.confirmBeforeUndo, v -> personal.confirmBeforeUndo = v);
        personalToggle(right, row + 119, columnWidth, "hud_hints", () -> personal.showHudHints, v -> personal.showHudHints = v);
        personalNumber(left, row + 141, columnWidth, "camera_zoom", () -> personal.maxCameraZoom, v -> personal.maxCameraZoom = v, 1, 6, 1);

        int footer = panelY + 209;
        int buttonWidth = Math.min(95, (panelWidth - 40) / 3);
        addRenderableWidget(Button.builder(Component.translatable("screen.realmfinder.save"), b -> save())
                .bounds(left, footer, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(panelX + (panelWidth - buttonWidth) / 2, footer, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.realmfinder.reset"), b -> resetDraft())
                .bounds(panelX + panelWidth - buttonWidth - 10, footer, buttonWidth, 20).build());
        for (Button button : ruleButtons) button.active = ServerSettings.received() && ServerSettings.editable();
    }

    private void ruleToggle(int x, int y, int w, String key, BooleanSupplier get, Consumer<Boolean> set) {
        Button button = toggle(x, y, w, key, get, set);
        ruleButtons.add(button);
    }

    private void personalToggle(int x, int y, int w, String key, BooleanSupplier get, Consumer<Boolean> set) {
        toggle(x, y, w, key, get, set);
    }

    private Button toggle(int x, int y, int w, String key, BooleanSupplier get, Consumer<Boolean> set) {
        Button button = Button.builder(label(key, get.getAsBoolean() ? "on" : "off"), b -> {
            set.accept(!get.getAsBoolean());
            b.setMessage(label(key, get.getAsBoolean() ? "on" : "off"));
        }).bounds(x, y, w, 20).tooltip(Tooltip.create(Component.translatable("screen.realmfinder.tip." + key))).build();
        addRenderableWidget(button);
        return button;
    }

    private void ruleNumber(int x, int y, int w, String key, IntSupplier get, IntConsumer set, int min, int max, int step) {
        ruleButtons.add(number(x, y, w, key, get, set, min, max, step));
    }

    private void personalNumber(int x, int y, int w, String key, IntSupplier get, IntConsumer set, int min, int max, int step) {
        number(x, y, w, key, get, set, min, max, step);
    }

    private Button number(int x, int y, int w, String key, IntSupplier get, IntConsumer set, int min, int max, int step) {
        Button button = Button.builder(label(key, Integer.toString(get.getAsInt())), b -> {
            int next;
            if (key.equals("blocks")) {
                int[] values = {256, 512, 1024, 2048, 4096, 8192, 16384};
                next = values[0];
                for (int value : values) {
                    if (value > get.getAsInt()) { next = value; break; }
                }
            } else {
                next = get.getAsInt() + step;
                if (next > max) next = min;
            }
            set.accept(next);
            b.setMessage(label(key, Integer.toString(get.getAsInt())));
        }).bounds(x, y, w, 20).tooltip(Tooltip.create(Component.translatable("screen.realmfinder.tip." + key))).build();
        addRenderableWidget(button);
        return button;
    }

    private static Component label(String key, String value) {
        return Component.translatable("screen.realmfinder.setting." + key)
                .append(": ").append(value.equals("on") || value.equals("off")
                        ? Component.translatable("screen.realmfinder.value." + value)
                        : Component.literal(value));
    }

    private void save() {
        ClientPreferences.save(personal);
        if (ServerSettings.received() && ServerSettings.editable())
            ClientPlayNetworking.send(new SettingsUpdatePayload(RealmFinderConfig.toJson(rules)));
        onClose();
    }

    private void resetDraft() {
        if (ServerSettings.editable()) rules = new RealmFinderConfig.Settings();
        personal = new ClientPreferences.Preferences();
        rebuildWidgets();
    }

    @Override public void onClose() { minecraft.setScreen(null); }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        if (!renderingWidgets) super.renderBackground(graphics);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics);
        graphics.fill(panelX - 3, panelY - 3, panelX + panelWidth + 3, panelY + 237, 0xFF171B1B);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 234, 0xFFE5DEC9);
        graphics.drawCenteredString(font, title, width / 2, panelY + 8, 0xFF334A43);
        graphics.drawString(font, Component.translatable("screen.realmfinder.gameplay"), panelX + 10, panelY + 24, 0xFF665940, false);
        graphics.drawString(font, Component.translatable("screen.realmfinder.personal"), panelX + 10, panelY + 145, 0xFF665940, false);
        if (!ServerSettings.received())
            graphics.drawString(font, Component.translatable("screen.realmfinder.loading"), panelX + 210, panelY + 24, 0xFF8A5A22, false);
        else if (!ServerSettings.editable())
            graphics.drawString(font, Component.translatable("screen.realmfinder.readonly"), panelX + 210, panelY + 24, 0xFF8A5A22, false);
        // Screen.render also calls renderBackground; keep it from blurring the panel.
        renderingWidgets = true;
        try {
            super.render(graphics, mouseX, mouseY, delta);
        } finally {
            renderingWidgets = false;
        }
    }
}
