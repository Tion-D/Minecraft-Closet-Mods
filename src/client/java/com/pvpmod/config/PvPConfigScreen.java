package com.pvpmod.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class PvPConfigScreen extends Screen {
    private final Screen parent;
    private final PvPConfig config;
    private int page = 0;
    private static final String[] PAGES = {"Aim", "Crits", "Shield", "HitSel", "Traj"};

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

    private void addToggle(int centerX, int y, int w, String label, boolean current, java.util.function.Consumer<Boolean> setter) {
        addRenderableWidget(Button.builder(
                Component.literal(label + ": " + (current ? "ON" : "OFF")),
                btn -> {
                    boolean newVal = !label.equals("Enabled") ? !current : !current;
                    boolean actualCurrent;
                    if (label.equals("Enabled") && this.page == 0) actualCurrent = config.aimAssistEnabled;
                    else if (label.equals("Vertical Assist")) actualCurrent = config.aimVerticalAssist;
                    else if (label.equals("Enabled") && this.page == 2) actualCurrent = config.shieldDisablerEnabled;
                    else if (label.equals("Enabled") && this.page == 4) actualCurrent = config.trajectoryEnabled;
                    else actualCurrent = current;
                    setter.accept(!actualCurrent);
                    init();
                })
                .bounds(centerX - w / 2, y, w, 20)
                .build());
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
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
