package com.pvpmod.modules;

import com.pvpmod.config.PvPConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class AimAssistModule {

    private UUID currentTargetUUID = null;
    private float targetYaw = 0f;
    private float targetPitch = 0f;
    private boolean hasTarget = false;

    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.screen != null) {
            hasTarget = false;
            return;
        }

        PvPConfig config = PvPConfig.getInstance();
        if (!config.aimAssistEnabled) {
            currentTargetUUID = null;
            hasTarget = false;
            return;
        }

        LocalPlayer player = client.player;

        List<? extends Player> nearbyPlayers = client.level.players().stream()
                .filter(p -> p != player)
                .filter(p -> !p.isSpectator())
                .filter(p -> !p.isDeadOrDying())
                .filter(p -> player.distanceTo(p) <= config.aimRadius)
                .filter(p -> !config.isFriend(p.getGameProfile().name()))
                .toList();

        if (nearbyPlayers.isEmpty()) {
            currentTargetUUID = null;
            hasTarget = false;
            return;
        }

        Player target = null;

        if (currentTargetUUID != null) {
            target = nearbyPlayers.stream()
                    .filter(p -> p.getUUID().equals(currentTargetUUID))
                    .findFirst()
                    .orElse(null);

            if (target != null && config.aimSnapAngle > 0) {
                if (getAngleTo(player, target) > config.aimSnapAngle) {
                    target = null;
                    currentTargetUUID = null;
                }
            }
        }

        if (target == null) {
            target = nearbyPlayers.stream()
                    .filter(p -> getAngleTo(player, p) <= config.aimFov / 2.0)
                    .min(Comparator.comparingDouble(p -> getAngleTo(player, p)))
                    .orElse(null);
        }

        if (target == null) {
            currentTargetUUID = null;
            hasTarget = false;
            return;
        }

        currentTargetUUID = target.getUUID();

        if (getAngleTo(player, target) > config.aimFov / 2.0) {
            hasTarget = false;
            return;
        }

        Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.45, 0);
        Vec3 targetVel = new Vec3(
            target.getX() - target.xOld,
            target.getY() - target.yOld,
            target.getZ() - target.zOld
        );
        targetPos = targetPos.add(targetVel.scale(config.aimSmoothing * 2.0));

        Vec3 eyePos = player.getEyePosition();
        Vec3 diff = targetPos.subtract(eyePos);

        double distance = diff.horizontalDistance();
        targetYaw = (float) Math.toDegrees(Math.atan2(-diff.x, diff.z));
        targetPitch = (float) Math.toDegrees(-Math.atan2(diff.y, distance));
        hasTarget = true;
    }

    public void onRender(float delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !hasTarget) return;

        PvPConfig config = PvPConfig.getInstance();
        if (!config.aimAssistEnabled) return;

        LocalPlayer player = client.player;

        float speed = (float) config.aimSpeed;
        float smoothing = (float) config.aimSmoothing;

        float frameSpeed = 1.0f - (float) Math.pow(1.0 - speed, delta);

        float yawDiff = wrapDegrees(targetYaw - player.getYRot());
        float pitchDiff = targetPitch - player.getXRot();

        float yawFactor = easeOut(Math.min(Math.abs(yawDiff) / 30f, 1f), smoothing);
        float pitchFactor = easeOut(Math.min(Math.abs(pitchDiff) / 30f, 1f), smoothing);

        float playerYawDelta = player.getYRot() - player.yRotO;
        float playerPitchDelta = player.getXRot() - player.xRotO;

        float yawResistance = 1.0f;
        float pitchResistance = 1.0f;

        if (Math.signum(playerYawDelta) != Math.signum(yawDiff) && Math.abs(playerYawDelta) > 0.5f) {
            yawResistance = 0.15f;
        }
        if (Math.signum(playerPitchDelta) != Math.signum(pitchDiff) && Math.abs(playerPitchDelta) > 0.5f) {
            pitchResistance = 0.15f;
        }

        float yawStep = yawDiff * frameSpeed * yawFactor * yawResistance;
        float pitchStep = pitchDiff * frameSpeed * pitchFactor * pitchResistance;

        if (Math.abs(yawDiff) < 0.02f) yawStep = yawDiff;
        if (Math.abs(pitchDiff) < 0.02f) pitchStep = pitchDiff;

        player.setYRot(player.getYRot() + yawStep);

        if (config.aimVerticalAssist) {
            float newPitch = player.getXRot() + pitchStep;
            player.setXRot(Math.max(-90f, Math.min(90f, newPitch)));
        }
    }

    private float easeOut(float t, float strength) {
        float curve = 1f + strength * 2f;
        return 1f - (float) Math.pow(1f - t, curve);
    }

    private double getAngleTo(LocalPlayer player, Player target) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 toTarget = target.position().add(0, target.getBbHeight() * 0.45, 0).subtract(eyePos).normalize();
        double dot = lookVec.dot(toTarget);
        dot = Math.max(-1.0, Math.min(1.0, dot));
        return Math.toDegrees(Math.acos(dot));
    }

    private float wrapDegrees(float degrees) {
        degrees = degrees % 360f;
        if (degrees >= 180f) degrees -= 360f;
        if (degrees < -180f) degrees += 360f;
        return degrees;
    }
}
