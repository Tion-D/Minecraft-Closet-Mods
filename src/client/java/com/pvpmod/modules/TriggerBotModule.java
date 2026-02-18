package com.pvpmod.modules;

import com.pvpmod.config.PvPConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.concurrent.ThreadLocalRandom;

public class TriggerBotModule {

    private int reactionTicks = 0;
    private Entity pendingTarget = null;
    private int attackCooldown = 0;

    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) return;

        PvPConfig config = PvPConfig.getInstance();
        if (!config.triggerBotEnabled) {
            reset();
            return;
        }

        LocalPlayer player = client.player;

        if (client.screen != null || player.isUsingItem()) return;

        if (attackCooldown > 0) {
            attackCooldown--;
            return;
        }

        Entity currentTarget = getTarget(client, player, config);

        if (currentTarget != null) {
            if (pendingTarget == null || pendingTarget.getId() != currentTarget.getId()) {
                pendingTarget = currentTarget;
                reactionTicks = rollReactionDelay(config);
            }

            if (reactionTicks > 0) {
                reactionTicks--;
                return;
            }

            Entity validatedTarget = getTarget(client, player, config);
            if (validatedTarget == null || validatedTarget.getId() != pendingTarget.getId()) {
                pendingTarget = null;
                return;
            }

            if (config.triggerBotSmartDelay) {
                float threshold = 0.9f + ThreadLocalRandom.current().nextFloat() * 0.1f;
                if (player.getAttackStrengthScale(0) < threshold) return;
            }

            client.gameMode.attack(player, validatedTarget);
            player.swing(InteractionHand.MAIN_HAND);
            player.resetAttackStrengthTicker();

            attackCooldown = config.triggerBotMinDelay + ThreadLocalRandom.current().nextInt(0, 2);

            pendingTarget = null;

        } else {
            pendingTarget = null;
            reactionTicks = 0;
        }
    }

    private Entity getTarget(Minecraft client, LocalPlayer player, PvPConfig config) {
        if (client.hitResult == null || client.hitResult.getType() != HitResult.Type.ENTITY) return null;

        Entity target = ((EntityHitResult) client.hitResult).getEntity();

        if (!target.isAlive() || target.isSpectator()) return null;
        if (target == player) return null;

        if (config.triggerBotPlayersOnly && !(target instanceof Player)) return null;

        if (target instanceof Player targetPlayer) {
            if (config.isFriend(targetPlayer.getGameProfile().name())) return null;
        }

        double distSq = player.distanceToSqr(target);
        if (distSq > config.triggerBotRange * config.triggerBotRange) return null;

        return target;
    }

    private int rollReactionDelay(PvPConfig config) {
        int min = config.triggerBotMinReaction;
        int max = config.triggerBotMaxReaction;
        if (min >= max) return min;
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private void reset() {
        reactionTicks = 0;
        pendingTarget = null;
        attackCooldown = 0;
    }
}