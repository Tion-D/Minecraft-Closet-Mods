package com.pvpmod.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class PvPConfigScreen extends Screen {
    private final Screen parent;
    private final PvPConfig config;
    private int page = 0;
    private boolean listeningForKey = false;
    private Button keyButton = null;
    private static final String[] PAGES = {"Aim", "Crits", "Shield", "HitSel", "Traj", "Totem", "Render", "ESP"};

    public PvPConfigScreen(Screen parent) {
        super(Component.literal("PvP Mod Settings"));
        this.parent = parent;
        this.config = PvPConfig.getInstance();
    }

    @Override
    protected void init() {
        clearWidgets();

        int centerX = this.width / 2;
        int y = 45;
        int buttonWidth = 200;

        int tabWidth = 60;
        int tabStart = centerX - (PAGES.length * tabWidth) / 2;
        for (int i = 0; i < PAGES.length; i++) {
            final int pageIndex = i;
            String label = (i == page ? "§a" : "§7") + PAGES[i];
            addRenderableWidget(Button.builder(Component.literal(label), btn -> {
                page = pageIndex;
                init();
            }).bounds(tabStart + i * tabWidth, 25, tabWidth - 2, 16).build());
        }

        switch (page) {
            case 0 -> initAimAssist(centerX, y, buttonWidth);
            case 1 -> initCriticals(centerX, y, buttonWidth);
            case 2 -> initShieldDisabler(centerX, y, buttonWidth);
            case 3 -> initHitSelect(centerX, y, buttonWidth);
            case 4 -> initTrajectory(centerX, y, buttonWidth);
            case 5 -> initAutoTotem(centerX, y, buttonWidth);
            case 6 -> initNoRender(centerX, y, buttonWidth);
            case 7 -> initESP(centerX, y, buttonWidth);
        }
    }

    private void initAimAssist(int centerX, int y, int w) {
        addToggle(centerX, y, w, "Enabled", config.aimAssistEnabled, v -> { config.aimAssistEnabled = v; config.save(); });
        y += 24;
        addSlider(centerX, y, w, "Radius", config.aimRadius, 1.0, 6.0, 1, v -> { config.aimRadius = v; config.save(); });
        y += 24;
        addSlider(centerX, y, w, "Aim Speed", config.aimSpeed, 0.01, 1.0, 2, v -> { config.aimSpeed = v; config.save(); });
        y += 24;
        addSlider(centerX, y, w, "Smoothing", config.aimSmoothing, 0.0, 1.0, 2, v -> { config.aimSmoothing = v; config.save(); });
        y += 24;
        addSlider(centerX, y, w, "FOV", config.aimFov, 10.0, 180.0, 0, v -> { config.aimFov = v; config.save(); });
        y += 24;
        addSlider(centerX, y, w, "Snap Angle", config.aimSnapAngle, 10.0, 180.0, 0, v -> { config.aimSnapAngle = v; config.save(); });
        y += 24;
        keyButton = addRenderableWidget(Button.builder(
                Component.literal("Toggle Key: " + getKeyName(config.aimToggleKeyCode)),
                btn -> {
                    listeningForKey = true;
                    btn.setMessage(Component.literal("Toggle Key: §e> Press a key <"));
                })
                .bounds(centerX - w / 2, y, w, 20)
                .build());
        y += 24;
        addToggle(centerX, y, w, "Vertical Assist", config.aimVerticalAssist, v -> { config.aimVerticalAssist = v; config.save(); });
        y += 30;
        addDoneButton(centerX, y);
    }

    private void initCriticals(int centerX, int y, int w) {
        addRenderableWidget(Button.builder(
                Component.literal("Enabled: " + (config.criticalsEnabled ? "ON" : "OFF")),
                btn -> {
                    config.criticalsEnabled = !config.criticalsEnabled;
                    config.save();
                    btn.setMessage(Component.literal("Enabled: " + (config.criticalsEnabled ? "ON" : "OFF")));
                })
                .bounds(centerX - w / 2, y, w, 20)
                .tooltip(Tooltip.create(Component.literal("Always off on startup")))
                .build());
        y += 24;
        addRenderableWidget(Button.builder(
                Component.literal("Mode: " + config.criticalsMode.toUpperCase()),
                btn -> {
                    config.criticalsMode = config.criticalsMode.equals("packet") ? "jump" : "packet";
                    config.save();
                    btn.setMessage(Component.literal("Mode: " + config.criticalsMode.toUpperCase()));
                })
                .bounds(centerX - w / 2, y, w, 20)
                .tooltip(Tooltip.create(Component.literal("Packet = invisible, Jump = visible but safer")))
                .build());
        y += 30;
        addDoneButton(centerX, y);
    }

    private void initShieldDisabler(int centerX, int y, int w) {
        addToggle(centerX, y, w, "Enabled", config.shieldDisablerEnabled, v -> { config.shieldDisablerEnabled = v; config.save(); });
        y += 24;
        addSlider(centerX, y, w, "Range", config.shieldRange, 1.0, 6.0, 1, v -> { config.shieldRange = v; config.save(); });
        y += 24;
        addSlider(centerX, y, w, "Swap Back Delay", config.shieldSwapBackDelay, 1.0, 5.0, 0, v -> { config.shieldSwapBackDelay = (int) v.doubleValue(); config.save(); });
        y += 30;
        addDoneButton(centerX, y);
    }

    private void initHitSelect(int centerX, int y, int w) {
        addRenderableWidget(Button.builder(
                Component.literal("Enabled: " + (config.hitSelectEnabled ? "ON" : "OFF")),
                btn -> {
                    config.hitSelectEnabled = !config.hitSelectEnabled;
                    config.save();
                    btn.setMessage(Component.literal("Enabled: " + (config.hitSelectEnabled ? "ON" : "OFF")));
                })
                .bounds(centerX - w / 2, y, w, 20)
                .tooltip(Tooltip.create(Component.literal("Always off on startup")))
                .build());
        y += 24;
        addSlider(centerX, y, w, "Combat Range", config.hitSelectCombatRange, 3.0, 10.0, 1, v -> { config.hitSelectCombatRange = v; config.save(); });
        y += 30;
        addDoneButton(centerX, y);
    }

    private void initTrajectory(int centerX, int y, int w) {
        addToggle(centerX, y, w, "Enabled", config.trajectoryEnabled, v -> { config.trajectoryEnabled = v; config.save(); });
        y += 30;
        addDoneButton(centerX, y);
    }

    private void initAutoTotem(int centerX, int y, int w) {
        addRenderableWidget(Button.builder(
                Component.literal("Enabled: " + (config.autoTotemEnabled ? "ON" : "OFF")),
                btn -> {
                    config.autoTotemEnabled = !config.autoTotemEnabled;
                    config.save();
                    btn.setMessage(Component.literal("Enabled: " + (config.autoTotemEnabled ? "ON" : "OFF")));
                })
                .bounds(centerX - w / 2, y, w, 20)
                .tooltip(Tooltip.create(Component.literal("Always off on startup")))
                .build());
        y += 24;

        addRenderableWidget(Button.builder(
                Component.literal("Mode: " + config.autoTotemMode.toUpperCase()),
                btn -> {
                    if (config.autoTotemMode.equals("offhand")) config.autoTotemMode = "hotbar";
                    else if (config.autoTotemMode.equals("hotbar")) config.autoTotemMode = "both";
                    else config.autoTotemMode = "offhand";
                    config.save();
                    btn.setMessage(Component.literal("Mode: " + config.autoTotemMode.toUpperCase()));
                })
                .bounds(centerX - w / 2, y, w, 20)
                .tooltip(Tooltip.create(Component.literal(
                    "Offhand = restock offhand when totem pops\n" +
                    "Hotbar = swap to totem slot when low HP\n" +
                    "Both = double totem (cpvp)"
                )))
                .build());
        y += 24;

        addSlider(centerX, y, w, "Hotbar Slot", config.autoTotemHotbarSlot + 1, 1.0, 9.0, 0,
                v -> { config.autoTotemHotbarSlot = (int) v.doubleValue() - 1; config.save(); });
        y += 24;

        addSlider(centerX, y, w, "Health Threshold", config.autoTotemHealthThreshold, 1.0, 20.0, 1,
                v -> { config.autoTotemHealthThreshold = v; config.save(); });
        y += 24;

        addToggle(centerX, y, w, "Elytra Protection", config.autoTotemElytra, v -> { config.autoTotemElytra = v; config.save(); });
        y += 24;

        addToggle(centerX, y, w, "Fall Protection", config.autoTotemFall, v -> { config.autoTotemFall = v; config.save(); });
        y += 24;

        addSlider(centerX, y, w, "Delay (ticks)", config.autoTotemDelay, 0.0, 5.0, 0,
                v -> { config.autoTotemDelay = (int) v.doubleValue(); config.save(); });
        y += 30;

        addDoneButton(centerX, y);
    }

    private void initNoRender(int centerX, int y, int w) {
        addToggle(centerX, y, w, "Enabled", config.noRenderEnabled, v -> { config.noRenderEnabled = v; config.save(); });
        y += 24;
        addToggle(centerX, y, w, "No Blindness", config.noBlindness, v -> { config.noBlindness = v; config.save(); });
        y += 24;
        addToggle(centerX, y, w, "No Darkness", config.noDarkness, v -> { config.noDarkness = v; config.save(); });
        y += 24;
        addToggle(centerX, y, w, "No Nausea", config.noNausea, v -> { config.noNausea = v; config.save(); });
        y += 24;
        addToggle(centerX, y, w, "No Fire Overlay", config.noFireOverlay, v -> { config.noFireOverlay = v; config.save(); });
        y += 24;
        addToggle(centerX, y, w, "No Water Overlay", config.noLiquidOverlay, v -> { config.noLiquidOverlay = v; config.save(); });
        y += 24;
        addToggle(centerX, y, w, "No Vignette", config.noVignette, v -> { config.noVignette = v; config.save(); });
        y += 24;
        addToggle(centerX, y, w, "No Scoreboard", config.noScoreboard, v -> { config.noScoreboard = v; config.save(); });
        y += 24;
        addToggle(centerX, y, w, "No Boss Bar", config.noBossBar, v -> { config.noBossBar = v; config.save(); });
        y += 30;
        addDoneButton(centerX, y);
    }

    private void initESP(int centerX, int y, int w) {
        addToggle(centerX, y, w, "ESP Enabled", config.espEnabled, v -> { config.espEnabled = v; config.save(); });
        y += 24;
        addToggle(centerX, y, w, "Ignore Self", config.espIgnoreSelf, v -> { config.espIgnoreSelf = v; config.save(); });
        y += 24;
        addToggle(centerX, y, w, "Nametags", config.espNametags, v -> { config.espNametags = v; config.save(); });
        y += 30;

        addRenderableWidget(Button.builder(Component.literal("§c--- Enemy Color ---"), btn -> {})
                .bounds(centerX - w / 2, y, w, 14).build()).active = false;
        y += 18;
        addSlider(centerX, y, w, "§cRed", config.espEnemyR, 0.0, 1.0, 2, v -> { config.espEnemyR = v.floatValue(); config.save(); });
        y += 24;
        addSlider(centerX, y, w, "§aGreen", config.espEnemyG, 0.0, 1.0, 2, v -> { config.espEnemyG = v.floatValue(); config.save(); });
        y += 24;
        addSlider(centerX, y, w, "§9Blue", config.espEnemyB, 0.0, 1.0, 2, v -> { config.espEnemyB = v.floatValue(); config.save(); });
        y += 30;

        addRenderableWidget(Button.builder(Component.literal("§a--- Friend Color ---"), btn -> {})
                .bounds(centerX - w / 2, y, w, 14).build()).active = false;
        y += 18;
        addSlider(centerX, y, w, "§cRed", config.espFriendR, 0.0, 1.0, 2, v -> { config.espFriendR = v.floatValue(); config.save(); });
        y += 24;
        addSlider(centerX, y, w, "§aGreen", config.espFriendG, 0.0, 1.0, 2, v -> { config.espFriendG = v.floatValue(); config.save(); });
        y += 24;
        addSlider(centerX, y, w, "§9Blue", config.espFriendB, 0.0, 1.0, 2, v -> { config.espFriendB = v.floatValue(); config.save(); });
        y += 30;

        addSlider(centerX, y, w, "Opacity", config.espAlpha, 0.1, 1.0, 2, v -> { config.espAlpha = v.floatValue(); config.save(); });
        y += 30;
        addDoneButton(centerX, y);
    }

    private void addToggle(int centerX, int y, int w, String label, boolean current, java.util.function.Consumer<Boolean> setter) {
        final boolean[] state = {current};
        addRenderableWidget(Button.builder(
                Component.literal(label + ": " + (state[0] ? "ON" : "OFF")),
                btn -> {
                    state[0] = !state[0];
                    setter.accept(state[0]);
                    btn.setMessage(Component.literal(label + ": " + (state[0] ? "ON" : "OFF")));
                })
                .bounds(centerX - w / 2, y, w, 20)
                .build());
    }

    private String getKeyName(int keyCode) {
        String name = GLFW.glfwGetKeyName(keyCode, 0);
        if (name != null) return name.toUpperCase();
        return "KEY_" + keyCode;
    }

    private void addSlider(int centerX, int y, int w, String label, double current, double min, double max, int decimals, java.util.function.Consumer<Double> setter) {
        addRenderableWidget(new AbstractSliderButton(centerX - w / 2, y, w, 20,
                Component.empty(), (current - min) / (max - min)) {
            {
                updateMessage();
            }

            @Override
            protected void updateMessage() {
                String format = "%." + decimals + "f";
                double val = min + value * (max - min);
                setMessage(Component.literal(label + ": " + String.format(format, val)));
            }

            @Override
            protected void applyValue() {
                setter.accept(min + value * (max - min));
            }
        });
    }

    private void addDoneButton(int centerX, int y) {
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> onClose())
                .bounds(centerX - 100, y, 200, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);

        if (listeningForKey) {
            long window = GLFW.glfwGetCurrentContext();
            for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
                if (GLFW.glfwGetKey(window, k) == GLFW.GLFW_PRESS) {
                    listeningForKey = false;
                    config.aimToggleKeyCode = k;
                    config.save();
                    if (keyButton != null) {
                        keyButton.setMessage(Component.literal("Toggle Key: " + getKeyName(k)));
                    }
                    break;
                }
            }
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
