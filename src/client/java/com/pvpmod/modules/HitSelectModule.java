package com.pvpmod.modules;

import com.pvpmod.config.PvPConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class HitSelectModule {

    public void onTickStart(Minecraft client) {
        if (client.player == null || client.level == null || client.screen != null) return;

        PvPConfig config = PvPConfig.getInstance();
        if (!config.hitSelectEnabled) return;

        LocalPlayer player = client.player;

        if (!client.options.keyAttack.isDown()) return;

        if (client.hitResult != null && client.hitResult.getType() == HitResult.Type.BLOCK) return;

        if (client.hitResult != null
                && client.hitResult.getType() == HitResult.Type.ENTITY
                && client.hitResult instanceof EntityHitResult) {
            return;
        }

        boolean playerNearby = client.level.players().stream()
                .filter(p -> p != player)
                .filter(p -> !p.isSpectator())
                .filter(p -> !p.isDeadOrDying())
                .anyMatch(p -> player.distanceTo(p) <= config.hitSelectCombatRange);

        if (!playerNearby) return;

        while (client.options.keyAttack.consumeClick()) {
        }
        client.options.keyAttack.setDown(false);
    }
}
