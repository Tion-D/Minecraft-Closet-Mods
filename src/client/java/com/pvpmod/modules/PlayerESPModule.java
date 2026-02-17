package com.pvpmod.modules;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pvpmod.config.PvPConfig;
import com.pvpmod.modules.trajectory.RenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

public class PlayerESPModule {

    public void onWorldRender(WorldRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        PvPConfig config = PvPConfig.getInstance();
        if (!config.espEnabled) return;

        Vec3 cam = client.gameRenderer.getMainCamera().position();
        PoseStack poseStack = context.matrices();
        MultiBufferSource consumers = context.consumers();
        if (consumers == null) return;

        float delta = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Quaternionf cameraRotation = client.gameRenderer.getMainCamera().rotation();

        client.renderBuffers().bufferSource().endBatch();

        GL11.glDisable(GL11.GL_DEPTH_TEST);

        for (Player player : client.level.players()) {
            if (player == client.player && config.espIgnoreSelf) continue;
            if (player.isSpectator() || player.isDeadOrDying()) continue;

            boolean isFriend = config.isFriend(player.getGameProfile().name());

            double x = lerp(delta, player.xOld, player.getX());
            double y = lerp(delta, player.yOld, player.getY());
            double z = lerp(delta, player.zOld, player.getZ());

            float hw = player.getBbWidth() / 2;
            float[] color = isFriend
                ? new float[]{config.espFriendR, config.espFriendG, config.espFriendB}
                : new float[]{config.espEnemyR, config.espEnemyG, config.espEnemyB};

            RenderUtils.renderBox(context,
                x - hw, y, z - hw,
                x + hw, y + player.getBbHeight(), z + hw,
                color, config.espAlpha);
                    }

        client.renderBuffers().bufferSource().endBatch();

        GL11.glEnable(GL11.GL_DEPTH_TEST);

        for (Player player : client.level.players()) {
            if (player == client.player && config.espIgnoreSelf) continue;
            if (player.isSpectator() || player.isDeadOrDying()) continue;
            if (!config.espNametags) continue;

            boolean isFriend = config.isFriend(player.getGameProfile().name());
            String name = player.getGameProfile().name();

            double x = lerp(delta, player.xOld, player.getX());
            double y = lerp(delta, player.yOld, player.getY());
            double z = lerp(delta, player.zOld, player.getZ());

            double dx = x - cam.x;
            double dy = y - cam.y;
            double dz = z - cam.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            float nametagScale = (float)(0.025 * Math.max(dist / 10.0, 1.0));

            poseStack.pushPose();
            poseStack.translate(dx, dy + player.getBbHeight() + 0.3, dz);
            poseStack.mulPose(cameraRotation);
            poseStack.scale(nametagScale, -nametagScale, nametagScale);

            Font font = client.font;
            float textX = -font.width(name) / 2f;
            int bgColor = (int)(0.4f * 255) << 24;
            int nameColor = isFriend
                ? (0xFF << 24) | ((int)(config.espFriendR * 255) << 16) | ((int)(config.espFriendG * 255) << 8) | (int)(config.espFriendB * 255)
                : (0xFF << 24) | ((int)(config.espEnemyR * 255) << 16) | ((int)(config.espEnemyG * 255) << 8) | (int)(config.espEnemyB * 255);

            font.drawInBatch(name, textX, 0, nameColor, false,
                poseStack.last().pose(), consumers, Font.DisplayMode.SEE_THROUGH,
                bgColor, 0xF000F0);

            poseStack.popPose();
        }
    }

    private double lerp(float delta, double old, double current) {
        return old + (current - old) * delta;
    }
}