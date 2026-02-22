package com.pvpmod.modules;

import com.pvpmod.config.PvPConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import com.pvpmod.mixin.MinecraftAccessor;

import java.util.concurrent.ThreadLocalRandom;

public class TriggerBotModule {

    private int attackCooldown = 0;

    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) return;

        PvPConfig config = PvPConfig.getInstance();
        if (!config.triggerBotEnabled) {
            attackCooldown = 0;
            return;
        }

        LocalPlayer player = client.player;

        if (client.screen != null || player.isUsingItem()) return;

        if (attackCooldown > 0) {
            attackCooldown--;
            return;
        }

        if (client.hitResult == null || client.hitResult.getType() != HitResult.Type.ENTITY) return;

        Entity target = ((EntityHitResult) client.hitResult).getEntity();

        if (!target.isAlive() || target.isSpectator() || target == player) return;
        if (config.triggerBotPlayersOnly && !(target instanceof Player)) return;

        if (target instanceof Player tp && config.isFriend(tp.getGameProfile().name())) return;

        double distSq = player.distanceToSqr(target);
        if (distSq > config.triggerBotRange * config.triggerBotRange) return;

        if (player.getAttackStrengthScale(0) < 0.5f) return;

        if (config.triggerBotSmartDelay) {
            float threshold = 0.95f + ThreadLocalRandom.current().nextFloat() * 0.05f;
            if (player.getAttackStrengthScale(0) < threshold) return;
        }

        ((MinecraftAccessor) client).invokeStartAttack();

        attackCooldown = config.triggerBotMinDelay;
    }
}