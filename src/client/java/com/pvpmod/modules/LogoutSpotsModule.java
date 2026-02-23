package com.pvpmod.modules;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pvpmod.config.PvPConfig;
import com.pvpmod.modules.trajectory.RenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.*;

public class LogoutSpotsModule {

    private final List<LogoutEntry> logoutSpots = new ArrayList<>();
    private final Set<UUID> lastPlayerUUIDs = new HashSet<>();
    private final Map<UUID, PlayerSnapshot> trackedPlayers = new HashMap<>();
    private int updateTimer = 0;
    private String lastDimension = null;

    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.getConnection() == null) return;

        PvPConfig config = PvPConfig.getInstance();
        if (!config.logoutSpotsEnabled) {
            logoutSpots.clear();
            lastPlayerUUIDs.clear();
            trackedPlayers.clear();
            return;
        }

        String dimension = client.level.dimension().toString();
        if (!dimension.equals(lastDimension)) {
            logoutSpots.clear();
            lastPlayerUUIDs.clear();
            trackedPlayers.clear();
            lastDimension = dimension;
        }

        if (updateTimer <= 0) {
            trackedPlayers.clear();
            for (Player player : client.level.players()) {
                if (player == client.player) continue;
                trackedPlayers.put(player.getUUID(), new PlayerSnapshot(
                        player.getUUID(),
                        player.getGameProfile().name(),
                        player.getX(), player.getY(), player.getZ(),
                        player.getBbWidth(), player.getBbHeight(),
                        player.getHealth() + player.getAbsorptionAmount(),
                        player.getMaxHealth() + player.getAbsorptionAmount()
                ));
            }
            updateTimer = 10;
        } else {
            updateTimer--;
        }

        Set<UUID> currentUUIDs = new HashSet<>();
        for (PlayerInfo info : client.getConnection().getListedOnlinePlayers()) {
            currentUUIDs.add(info.getProfile().id());
        }

        if (!lastPlayerUUIDs.isEmpty()) {
            for (UUID uuid : lastPlayerUUIDs) {
                if (!currentUUIDs.contains(uuid) && !uuid.equals(client.player.getUUID())) {
                    PlayerSnapshot snapshot = trackedPlayers.get(uuid);
                    if (snapshot != null) {
                        logoutSpots.removeIf(e -> e.uuid.equals(uuid));
                        logoutSpots.add(new LogoutEntry(snapshot));
                    }
                }
            }
        }

        lastPlayerUUIDs.clear();
        lastPlayerUUIDs.addAll(currentUUIDs);

        for (Player player : client.level.players()) {
            logoutSpots.removeIf(e -> e.uuid.equals(player.getUUID()));
        }
    }

    public void onWorldRender(WorldRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        PvPConfig config = PvPConfig.getInstance();
        if (!config.logoutSpotsEnabled || logoutSpots.isEmpty()) return;

        Vec3 cam = client.gameRenderer.getMainCamera().position();
        PoseStack poseStack = context.matrices();
        MultiBufferSource consumers = context.consumers();
        if (consumers == null) return;

        Quaternionf cameraRotation = client.gameRenderer.getMainCamera().rotation();

        for (LogoutEntry entry : logoutSpots) {
            double dist = cam.distanceTo(new Vec3(entry.x + entry.halfWidth, entry.y, entry.z + entry.halfWidth));
            if (dist > 256) continue;

            float[] color = getHealthColor(entry.health, entry.maxHealth);
            RenderUtils.renderBox(context,
                entry.x, entry.y, entry.z,
                entry.x + entry.bbWidth, entry.y + entry.bbHeight, entry.z + entry.bbWidth,
                color, 0.7f);

            double dx = (entry.x + entry.halfWidth) - cam.x;
            double dy = entry.y - cam.y;
            double dz = (entry.z + entry.halfWidth) - cam.z;

            float nametagScale = (float) (0.025 * Math.max(dist / 10.0, 1.0));

            poseStack.pushPose();
            poseStack.translate(dx, dy + entry.bbHeight + 0.3, dz);
            poseStack.mulPose(cameraRotation);
            poseStack.scale(nametagScale, -nametagScale, nametagScale);

            Font font = client.font;
            int healthPercent = (int) ((entry.health / entry.maxHealth) * 100);
            String text = entry.name + " §7[§" + getHealthColorCode(entry.health, entry.maxHealth) + healthPercent + "%§7]";

            float textWidth = font.width(text);
            float textX = -textWidth / 2f;

            int bgColor = (int) (0.5f * 255) << 24;

            int textColor = 0xFFFFFFFF;
            font.drawInBatch(text, textX, 0, textColor, false,
                    poseStack.last().pose(), consumers, Font.DisplayMode.SEE_THROUGH,
                    bgColor, 0xF000F0);

            poseStack.popPose();
        }
    }

    private float[] getHealthColor(float health, float maxHealth) {
        float pct = health / maxHealth;
        if (pct <= 0.333f) return new float[]{0.9f, 0.1f, 0.1f};
        if (pct <= 0.666f) return new float[]{0.9f, 0.4f, 0.1f};
        return new float[]{0.1f, 0.9f, 0.1f};
    }

    private char getHealthColorCode(float health, float maxHealth) {
        float pct = health / maxHealth;
        if (pct <= 0.333f) return 'c';
        if (pct <= 0.666f) return '6';
        return 'a';
    }

    private static class PlayerSnapshot {
        final UUID uuid;
        final String name;
        final double x, y, z;
        final float bbWidth, bbHeight;
        final float health, maxHealth;

        PlayerSnapshot(UUID uuid, String name, double x, double y, double z,
                       float bbWidth, float bbHeight, float health, float maxHealth) {
            this.uuid = uuid;
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.bbWidth = bbWidth;
            this.bbHeight = bbHeight;
            this.health = health;
            this.maxHealth = maxHealth;
        }
    }

    private static class LogoutEntry {
        final UUID uuid;
        final String name;
        final double x, y, z;
        final float bbWidth, bbHeight, halfWidth;
        final float health, maxHealth;
        final long timestamp;

        LogoutEntry(PlayerSnapshot snapshot) {
            this.uuid = snapshot.uuid;
            this.name = snapshot.name;
            this.x = snapshot.x - snapshot.bbWidth / 2.0;
            this.y = snapshot.y;
            this.z = snapshot.z - snapshot.bbWidth / 2.0;
            this.bbWidth = snapshot.bbWidth;
            this.bbHeight = snapshot.bbHeight;
            this.halfWidth = snapshot.bbWidth / 2.0f;
            this.health = snapshot.health;
            this.maxHealth = snapshot.maxHealth;
            this.timestamp = System.currentTimeMillis();
        }
    }
}