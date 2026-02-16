package com.pvpmod.modules;

import com.pvpmod.config.PvPConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

public class CriticalsModule {
    private boolean waitingForPeak = false;
    private boolean sendingAttack = false;
    private double lastY = 0;
    private Entity pendingTarget = null;

    public InteractionResult onAttack(Player player, Level world, InteractionHand hand, Entity entity, Object hitResult) {
        if (!world.isClientSide()) return InteractionResult.PASS;

        PvPConfig config = PvPConfig.getInstance();
        if (!config.criticalsEnabled) return InteractionResult.PASS;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer localPlayer = mc.player;
        if (localPlayer == null || localPlayer != player) return InteractionResult.PASS;

        if (!(entity instanceof LivingEntity)) return InteractionResult.PASS;

        if (skipCrit(localPlayer, config)) return InteractionResult.PASS;

        double x = localPlayer.getX();
        double y = localPlayer.getY();
        double z = localPlayer.getZ();

        if (config.criticalsMode.equals("packet")) {
            localPlayer.connection.send(new ServerboundMovePlayerPacket.Pos(x, y + 0.0625, z, false, false));
            localPlayer.connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, false, false));
            return InteractionResult.PASS;
        } else if (config.criticalsMode.equals("jump")) {
            if (!sendingAttack) {
                sendingAttack = true;
                pendingTarget = entity;
                localPlayer.jumpFromGround();
                waitingForPeak = true;
                lastY = localPlayer.getY();
                return InteractionResult.FAIL;
            }
            return InteractionResult.FAIL;
        }

        return InteractionResult.PASS;
    }

    public void onTick(Minecraft client) {
        if (!sendingAttack) return;
        if (client.player == null) {
            reset();
            return;
        }

        PvPConfig config = PvPConfig.getInstance();
        if (!config.criticalsEnabled) {
            reset();
            return;
        }

        if (waitingForPeak) {
            double currentY = client.player.getY();
            if (currentY <= lastY) {
                waitingForPeak = false;
            }
            lastY = currentY;
            return;
        }

        if (pendingTarget != null && client.gameMode != null
                && pendingTarget.isAlive()
                && client.player.distanceTo(pendingTarget) <= 6.0) {
            client.gameMode.attack(client.player, pendingTarget);
            client.player.swing(InteractionHand.MAIN_HAND);
        }

        reset();
    }

    private void reset() {
        pendingTarget = null;
        sendingAttack = false;
        waitingForPeak = false;
        lastY = 0;
    }

    private boolean skipCrit(LocalPlayer player, PvPConfig config) {
        if (config.criticalsMode.equals("jump") && player.isInWall()) {
            return true;
        }

        return !player.onGround()
                || player.isUnderWater()
                || player.isInLava()
                || player.onClimbable();
    }
}