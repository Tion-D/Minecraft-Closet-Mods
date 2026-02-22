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
    private Player currentTarget = null;
    private Vec3 lastTargetPos = null;

    // GCD
    private double cachedGCD = 0;
    private double lastSensitivity = -1;

    // Residual accumulation (prevents jitter)
    private float yawResidue = 0f;
    private float pitchResidue = 0f;

    // Rotation tracking for legit mode
    private float lastYaw = 0f;
    private float lastPitch = 0f;
    private boolean trackingInitialized = false;

    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.screen != null) {
            clearTarget();
            return;
        }

        PvPConfig config = PvPConfig.getInstance();
        if (!config.aimAssistEnabled) {
            clearTarget();
            return;
        }

        LocalPlayer player = client.player;
        updateGCD(client);

        List<? extends Player> candidates = client.level.players().stream()
                .filter(p -> p != player)
                .filter(p -> !p.isSpectator())
                .filter(p -> !p.isDeadOrDying())
                .filter(p -> player.distanceTo(p) <= config.aimRadius)
                .filter(p -> !config.isFriend(p.getGameProfile().name()))
                .filter(p -> getAngleTo(player, p) <= config.aimFov / 2.0)
                .toList();

        Player best = null;

        if (currentTargetUUID != null) {
            best = candidates.stream()
                    .filter(p -> p.getUUID().equals(currentTargetUUID))
                    .findFirst()
                    .orElse(null);

            if (best != null && getAngleTo(player, best) > config.aimSnapAngle) {
                best = null;
            }
        }

        if (best == null) {
            best = candidates.stream()
                    .min(Comparator.comparingDouble(p -> getAngleTo(player, p)))
                    .orElse(null);
        }

        if (best == null) {
            clearTarget();
            return;
        }

        if (!best.getUUID().equals(currentTargetUUID)) {
            lastTargetPos = null;
            trackingInitialized = false;
            yawResidue = 0f;
            pitchResidue = 0f;
        }

        currentTarget = best;
        currentTargetUUID = best.getUUID();
    }

    public void onRender(float delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || currentTarget == null) return;

        if (!currentTarget.isAlive() || currentTarget.isRemoved()) {
            clearTarget();
            return;
        }

        PvPConfig config = PvPConfig.getInstance();
        if (!config.aimAssistEnabled) return;

        if (config.aimRageMode) {
            renderRage(client.player, config, delta);
        } else {
            renderLegit(client.player, config, delta);
        }
    }

    // ==========================================
    // RAGE — direct pull toward target
    // ==========================================
    private void renderRage(LocalPlayer player, PvPConfig config, float delta) {
        Vec3 targetPos = getTargetAimPos(currentTarget);
        Vec3 eyePos = player.getEyePosition();
        Vec3 diff = targetPos.subtract(eyePos);

        float idealYaw = (float) Math.toDegrees(Math.atan2(-diff.x, diff.z));
        float idealPitch = (float) Math.toDegrees(-Math.atan2(diff.y, diff.horizontalDistance()));

        float yawDiff = wrapDegrees(idealYaw - player.getYRot());
        float pitchDiff = idealPitch - player.getXRot();

        if (Math.abs(yawDiff) < 0.05f && Math.abs(pitchDiff) < 0.05f) return;

        float speed = (float) config.aimSpeed;
        float frameSpeed = 1.0f - (float) Math.pow(1.0 - speed, delta);

        float rawYaw = yawDiff * frameSpeed;
        float rawPitch = pitchDiff * frameSpeed;

        // Accumulate + snap
        yawResidue += rawYaw;
        pitchResidue += rawPitch;

        float yawStep = snapToGCD(yawResidue);
        float pitchStep = snapToGCD(pitchResidue);

        yawResidue -= yawStep;
        pitchResidue -= pitchStep;

        yawResidue = clamp(yawResidue, -2f, 2f);
        pitchResidue = clamp(pitchResidue, -2f, 2f);

        if (yawStep != 0f) {
            player.setYRot(player.getYRot() + yawStep);
        }

        if (config.aimVerticalAssist && pitchStep != 0f) {
            player.setXRot(clamp(player.getXRot() + pitchStep, -90f, 90f));
        }
    }

    // ==========================================
    // LEGIT — friction + pull + target tracking
    // ==========================================
    private void renderLegit(LocalPlayer player, PvPConfig config, float delta) {
        if (!trackingInitialized) {
            lastYaw = player.getYRot();
            lastPitch = player.getXRot();
            lastTargetPos = getTargetAimPos(currentTarget);
            trackingInitialized = true;
            return;
        }

        float playerYawDelta = player.getYRot() - lastYaw;
        float playerPitchDelta = player.getXRot() - lastPitch;
        boolean mouseMoving = Math.abs(playerYawDelta) > 0.01f || Math.abs(playerPitchDelta) > 0.01f;

        Vec3 targetPos = getTargetAimPos(currentTarget);
        Vec3 eyePos = player.getEyePosition();
        Vec3 diff = targetPos.subtract(eyePos);

        float idealYaw = (float) Math.toDegrees(Math.atan2(-diff.x, diff.z));
        float idealPitch = (float) Math.toDegrees(-Math.atan2(diff.y, diff.horizontalDistance()));

        float yawToTarget = wrapDegrees(idealYaw - player.getYRot());
        float pitchToTarget = idealPitch - player.getXRot();

        // Closeness: 1.0 = on target, 0.0 = FOV edge
        double angle = getAngleTo(player, currentTarget);
        double halfFov = config.aimFov / 2.0;
        float closeness = (float) Math.max(0, 1.0 - (angle / halfFov));
        closeness = closeness * closeness;

        float yawAdjust = 0f;
        float pitchAdjust = 0f;

        // PULL — gentle nudge toward target
        float pullStrength = (float) config.aimSpeed * closeness * delta * 0.4f;
        yawAdjust += yawToTarget * pullStrength;
        pitchAdjust += pitchToTarget * pullStrength;

        // FRICTION — slow crosshair near target
        if (mouseMoving) {
            float frictionStrength = (float) config.aimSpeed * closeness * 0.3f;
            yawAdjust += -playerYawDelta * frictionStrength;
            pitchAdjust += -playerPitchDelta * frictionStrength;
        }

        // TARGET COMPENSATION — follow strafing
        if (lastTargetPos != null) {
            Vec3 targetDelta = targetPos.subtract(lastTargetPos);

            if (targetDelta.lengthSqr() > 0.0001) {
                Vec3 diffOld = lastTargetPos.subtract(eyePos);
                float oldYaw = (float) Math.toDegrees(Math.atan2(-diffOld.x, diffOld.z));
                float oldPitch = (float) Math.toDegrees(-Math.atan2(diffOld.y, diffOld.horizontalDistance()));

                float yawShift = wrapDegrees(idealYaw - oldYaw);
                float pitchShift = idealPitch - oldPitch;

                float trackStrength = (float) config.aimSmoothing * closeness;
                yawAdjust += yawShift * trackStrength;
                pitchAdjust += pitchShift * trackStrength;
            }
        }

        lastTargetPos = targetPos;

        // Accumulate + snap
        yawResidue += yawAdjust;
        pitchResidue += pitchAdjust;

        float yawStep = snapToGCD(yawResidue);
        float pitchStep = snapToGCD(pitchResidue);

        yawResidue -= yawStep;
        pitchResidue -= pitchStep;

        yawResidue = clamp(yawResidue, -2f, 2f);
        pitchResidue = clamp(pitchResidue, -2f, 2f);

        if (yawStep != 0f) {
            player.setYRot(player.getYRot() + yawStep);
        }

        if (config.aimVerticalAssist && pitchStep != 0f) {
            player.setXRot(clamp(player.getXRot() + pitchStep, -90f, 90f));
        }

        lastYaw = player.getYRot();
        lastPitch = player.getXRot();
    }

    private Vec3 getTargetAimPos(Player target) {
        return target.getEyePosition().add(0, -target.getBbHeight() * 0.15, 0);
    }

    private void clearTarget() {
        currentTarget = null;
        currentTargetUUID = null;
        lastTargetPos = null;
        trackingInitialized = false;
        yawResidue = 0f;
        pitchResidue = 0f;
    }

    private void updateGCD(Minecraft client) {
        double sensitivity = client.options.sensitivity().get();
        if (sensitivity != lastSensitivity) {
            double f = sensitivity * 0.6 + 0.2;
            cachedGCD = f * f * f * 8.0;
            lastSensitivity = sensitivity;
        }
    }

    private float snapToGCD(float value) {
        if (cachedGCD <= 0) return value;
        return (float) (Math.round(value / cachedGCD) * cachedGCD);
    }

    private double getAngleTo(LocalPlayer player, Player target) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 toTarget = getTargetAimPos(target).subtract(eyePos).normalize();
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

    private float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
}