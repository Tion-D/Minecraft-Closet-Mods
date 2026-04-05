package com.pvpmod.modules;

import com.pvpmod.config.PvPConfig;
import com.pvpmod.mixin.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;

import java.util.concurrent.ThreadLocalRandom;

public class FastMendModule {

    private static final int VANILLA_DELAY = 4;
    private boolean wasActive = false;

    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;

        PvPConfig config = PvPConfig.getInstance();
        if (!config.fastMendEnabled) {
            restore(client);
            return;
        }

        LocalPlayer player = client.player;

        if (!client.options.keyUse.isDown()) {
            restore(client);
            return;
        }

        if (player.isUsingItem()) {
            restore(client);
            return;
        }

        boolean mainHandXP = player.getMainHandItem().is(Items.EXPERIENCE_BOTTLE);
        boolean offhandXP = player.getOffhandItem().is(Items.EXPERIENCE_BOTTLE);

        if (!mainHandXP && !offhandXP) {
            restore(client);
            return;
        }

        if (!mainHandXP && offhandXP) {
            var mainItem = player.getMainHandItem();
            if (!mainItem.isEmpty()) {
                restore(client);
                return;
            }
        }

        MinecraftAccessor accessor = (MinecraftAccessor) client;
        if (accessor.getRightClickDelay() > config.fastMendDelay) {
            int jitter = ThreadLocalRandom.current().nextInt(0, 2);
            accessor.setRightClickDelay(config.fastMendDelay + jitter);
        }
        wasActive = true;
    }

    private void restore(Minecraft client) {
        if (wasActive) {
            MinecraftAccessor accessor = (MinecraftAccessor) client;
            if (accessor.getRightClickDelay() < VANILLA_DELAY) {
                accessor.setRightClickDelay(VANILLA_DELAY);
            }
            wasActive = false;
        }
    }
}
