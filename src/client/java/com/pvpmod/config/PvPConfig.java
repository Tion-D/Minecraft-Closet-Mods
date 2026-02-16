package com.pvpmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PvPConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "pvpmod.json");

    private static PvPConfig INSTANCE = null;

    // ===== Aim Assist =====
    public boolean aimAssistEnabled = true;
    public double aimRadius = 3.2;
    public double aimFov = 90.0;
    public double aimSnapAngle = 25.0;
    public double aimSpeed = 1;
    public double aimSmoothing = 0.7;
    public boolean aimVerticalAssist = false;

    // ===== Criticals =====
    public boolean criticalsEnabled = false;
    public String criticalsMode = "packet";

    // ===== Shield Disabler =====
    public boolean shieldDisablerEnabled = true;
    public double shieldRange = 3;
    public int shieldSwapBackDelay = 2;

    // ===== Hit Select =====
    public boolean hitSelectEnabled = true;
    public double hitSelectCombatRange = 6.0;

    // ===== Trajectory =====
    public boolean trajectoryEnabled = true;

    // ===== Friends =====
    public List<String> friends = new ArrayList<>();

    public boolean isFriend(String name) {
        return friends.stream().anyMatch(f -> f.equalsIgnoreCase(name));
    }

    public boolean addFriend(String name) {
        if (isFriend(name)) return false;
        friends.add(name);
        save();
        return true;
    }

    public boolean removeFriend(String name) {
        boolean removed = friends.removeIf(f -> f.equalsIgnoreCase(name));
        if (removed) save();
        return removed;
    }

    public static PvPConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public static PvPConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                PvPConfig config = GSON.fromJson(reader, PvPConfig.class);
                if (config != null) {
                    config.criticalsEnabled = false;
                    config.hitSelectEnabled = true;
                    return config;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        PvPConfig config = new PvPConfig();
        config.save();
        return config;
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
