package com.pvpmod.modules;

import com.pvpmod.config.PvPConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class FreecamModule {

    private boolean active = false;
    private boolean keyWasPressed = false;

    private double camX, camY, camZ;
    private float camYaw, camPitch;
    private double prevCamX, prevCamY, prevCamZ;
    private float prevCamYaw, prevCamPitch;

    private float playerYaw, playerPitch;

    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            if (active) deactivate(client);
            return;
        }

        PvPConfig config = PvPConfig.getInstance();
        long window = GLFW.glfwGetCurrentContext();

        if (client.screen == null) {
            boolean pressed = GLFW.glfwGetKey(window, config.freecamKeyCode) == GLFW.GLFW_PRESS;
            if (pressed && !keyWasPressed) {
                if (active) {
                    deactivate(client);
                } else {
                    activate(client);
                }
            }
            keyWasPressed = pressed;
        }

        if (!active) return;

        prevCamX = camX;
        prevCamY = camY;
        prevCamZ = camZ;
        prevCamYaw = camYaw;
        prevCamPitch = camPitch;

        double speed = config.freecamSpeed;

        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS) speed *= 2.0;

        double forward = 0, strafe = 0, vertical = 0;

        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS) forward += 1;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS) forward -= 1;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS) strafe += 1;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS) strafe -= 1;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS) vertical += 1;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS) vertical -= 1;

        double len = Math.sqrt(forward * forward + strafe * strafe);
        if (len > 0) {
            forward /= len;
            strafe /= len;
        }

        double rad = Math.toRadians(camYaw);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);

        camX -= (forward * sin + strafe * cos) * speed;
        camZ += (forward * cos - strafe * sin) * speed;
        camY += vertical * speed;

        LocalPlayer player = client.player;
        player.setDeltaMovement(Vec3.ZERO);
        player.setOnGround(true);

        client.options.keyUp.setDown(false);
        client.options.keyDown.setDown(false);
        client.options.keyLeft.setDown(false);
        client.options.keyRight.setDown(false);
        client.options.keyJump.setDown(false);
        client.options.keyShift.setDown(false);
    }

    public void onMouseMoveRaw(float dx, float dy) {
        if (!active) return;
        camYaw += dx;
        camPitch += dy;
        camPitch = Math.clamp(camPitch, -90f, 90f);
    }

    private void activate(Minecraft client) {
        active = true;

        Vec3 camPos = client.gameRenderer.getMainCamera().position();
        camX = camPos.x;
        camY = camPos.y;
        camZ = camPos.z;
        prevCamX = camX;
        prevCamY = camY;
        prevCamZ = camZ;

        camYaw = client.player.getYRot();
        camPitch = client.player.getXRot();
        prevCamYaw = camYaw;
        prevCamPitch = camPitch;

        playerYaw = client.player.getYRot();
        playerPitch = client.player.getXRot();

        client.player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("Freecam: §aON"), true
        );
    }

    private void deactivate(Minecraft client) {
        active = false;

        if (client.player != null) {
            client.player.setYRot(playerYaw);
            client.player.setXRot(playerPitch);
            client.player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("Freecam: §cOFF"), true
            );
        }
    }

    public boolean isActive() {
        return active;
    }

    public double getX(float delta) {
        return prevCamX + (camX - prevCamX) * delta;
    }

    public double getY(float delta) {
        return prevCamY + (camY - prevCamY) * delta;
    }

    public double getZ(float delta) {
        return prevCamZ + (camZ - prevCamZ) * delta;
    }

    public float getYaw(float delta) {
        return prevCamYaw + (camYaw - prevCamYaw) * delta;
    }

    public float getPitch(float delta) {
        return prevCamPitch + (camPitch - prevCamPitch) * delta;
    }
}