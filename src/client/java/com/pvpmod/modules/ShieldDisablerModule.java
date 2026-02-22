package com.pvpmod.modules;

import com.pvpmod.config.PvPConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import com.pvpmod.mixin.MinecraftAccessor;

public class ShieldDisablerModule {

    private enum State { IDLE, SWAPPED, ATTACKING, SWAPPING_BACK }

    private State state = State.IDLE;
    private int originalSlot = -1;
    private int timer = 0;
    private Player pendingTarget = null;

    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.screen != null) return;

        PvPConfig config = PvPConfig.getInstance();
        if (!config.shieldDisablerEnabled) {
            resetState(client.player);
            return;
        }

        LocalPlayer player = client.player;

        switch (state) {
            case IDLE -> {
                if (client.hitResult == null) return;
                if (client.hitResult.getType() != HitResult.Type.ENTITY) return;
                if (!(client.hitResult instanceof EntityHitResult entityHit)) return;
                if (!(entityHit.getEntity() instanceof Player target)) return;
                if (target == player) return;
                if (config.isFriend(target.getGameProfile().name())) return;
                if (!target.isBlocking()) return;
                if (player.distanceTo(target) > config.shieldRange) return;

                int axeSlot = findAxeSlot(player);
                if (axeSlot == -1) return;

                if (axeSlot == getSlot(player)) {
                    simulateAttack(client, target);
                    return;
                }

                originalSlot = getSlot(player);
                setSlot(player, axeSlot);
                pendingTarget = target;
                state = State.SWAPPED;
                timer = 1;
            }

            case SWAPPED -> {
                timer--;
                if (timer <= 0) {
                    if (pendingTarget != null
                            && pendingTarget.isAlive()
                            && player.distanceTo(pendingTarget) <= config.shieldRange) {
                        simulateAttack(client, pendingTarget);
                    }
                    pendingTarget = null;
                    state = State.ATTACKING;
                    timer = config.shieldSwapBackDelay;
                }
            }

            case ATTACKING -> {
                timer--;
                if (timer <= 0) {
                    if (originalSlot >= 0 && originalSlot < 9) {
                        setSlot(player, originalSlot);
                    }
                    state = State.SWAPPING_BACK;
                    timer = 1;
                }
            }

            case SWAPPING_BACK -> {
                timer--;
                if (timer <= 0) {
                    originalSlot = -1;
                    state = State.IDLE;
                }
            }
        }
    }

    private void resetState(LocalPlayer player) {
        if (state != State.IDLE && originalSlot >= 0 && originalSlot < 9) {
            setSlot(player, originalSlot);
        }
        state = State.IDLE;
        originalSlot = -1;
        timer = 0;
        pendingTarget = null;
    }

    private void simulateAttack(Minecraft client, Player target) {
        // if (client.gameMode == null) return;
        // if (client.player == null) return;
        // client.gameMode.attack(client.player, target);
        // client.player.swing(InteractionHand.MAIN_HAND);
        ((MinecraftAccessor) client).invokeStartAttack();
    }

    private int findAxeSlot(LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).getItem() instanceof AxeItem) {
                return i;
            }
        }
        return -1;
    }

    private int getSlot(LocalPlayer player) {
        return player.getInventory().getSelectedSlot();
    }

    private void setSlot(LocalPlayer player, int slot) {
        player.getInventory().setSelectedSlot(slot);
    }
}
