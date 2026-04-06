package com.pvpmod.modules;

import com.pvpmod.config.PvPConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.effect.MobEffects;

import java.util.concurrent.ThreadLocalRandom;

public class AutoTotemModule {

    private enum State {
        IDLE,
        HOTBAR_SWITCHED,
        HOTBAR_SWAPPED
    }

    private State state = State.IDLE;
    private int originalSlot = -1;
    private boolean lastOffhandWasTotem = false;
    private boolean totemJustPopped = false;
    private int tickDelay = 0;

    private int lastTrackedSlot = -1;
    private int playerOverrideCooldown = 0;
    private static final int OVERRIDE_COOLDOWN_TICKS = 15;

    private boolean swapInFlight = false;
    private int swapInFlightTimeout = 0;

    // Anti-ghost totem
    private int syncCountdown = 0;

    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;

        PvPConfig config = PvPConfig.getInstance();
        if (!config.autoTotemEnabled) {
            resetAll(client);
            return;
        }

        LocalPlayer player = client.player;

        antiGhostTotem(client, player);

        boolean offhandIsTotem = isTotem(player.getOffhandItem());
        int currentSlot = getSlot(player);

        // Detect manual slot change by player
        if (state == State.IDLE && lastTrackedSlot >= 0 && currentSlot != lastTrackedSlot) {
            playerOverrideCooldown = OVERRIDE_COOLDOWN_TICKS;
        }
        lastTrackedSlot = currentSlot;

        if (playerOverrideCooldown > 0) {
            playerOverrideCooldown--;
        }

        if (state == State.IDLE) {
            if (lastOffhandWasTotem && !offhandIsTotem && player.getOffhandItem().isEmpty()) {
                float health = player.getHealth();

                if (health <= 3.0f || player.hasEffect(MobEffects.REGENERATION)) {
                    // Real totem pop — low health or regen means it just saved us
                    totemJustPopped = true;
                    playerOverrideCooldown = 0;
                } else {
                    totemJustPopped = true;
                    playerOverrideCooldown = 0;
                    tickDelay = 20 + ThreadLocalRandom.current().nextInt(0, 41);
                }
            }
        }

        if (state != State.IDLE) {
            handleState(client, player, config);
            lastOffhandWasTotem = isTotem(player.getOffhandItem());
            lastTrackedSlot = getSlot(player);
            return;
        }

        if (tickDelay > 0) {
            tickDelay--;
            if (swapInFlight && isTotem(player.getOffhandItem())) {
                swapInFlight = false;
            }
            lastOffhandWasTotem = offhandIsTotem;
            return;
        }

        if (swapInFlight) {
            if (isTotem(player.getOffhandItem())) {
                swapInFlight = false;
            } else if (--swapInFlightTimeout <= 0) {
                swapInFlight = false;
            } else {
                lastOffhandWasTotem = offhandIsTotem;
                return;
            }
        }

        boolean usingItem = player.isUsingItem();
        String mode = config.autoTotemMode;
        boolean forceTotem = shouldForceTotem(player, config);

        // Cancel eating/using if totem needed urgently
        if (usingItem && !offhandIsTotem && (forceTotem || totemJustPopped)) {
            client.options.keyUse.setDown(false);
            player.releaseUsingItem();
            lastOffhandWasTotem = offhandIsTotem;
            return;
        }

        if ((mode.equals("hotbar") || mode.equals("both")) && !usingItem) {
            if (forceTotem && !isTotem(player.getMainHandItem())) {
                if (playerOverrideCooldown <= 0) {
                    int totemSlot = findTotemInHotbar(player);
                    if (totemSlot != -1) {
                        setSlot(player, totemSlot);
                        lastTrackedSlot = totemSlot;
                        tickDelay = config.autoTotemDelay;
                        totemJustPopped = false;
                        lastOffhandWasTotem = offhandIsTotem;
                        return;
                    }
                }
            }
        }

        if (mode.equals("offhand") || mode.equals("both")) {
            if (!offhandIsTotem) {
                boolean offhandEmpty = player.getOffhandItem().isEmpty();
                boolean shouldRestock = false;

                if (mode.equals("both") && offhandEmpty) {
                    shouldRestock = true;
                } else if (mode.equals("offhand")) {
                    if ((totemJustPopped || forceTotem) && offhandEmpty) {
                        shouldRestock = true;
                    }
                }

                if (shouldRestock) {
                    int hotbarSlot = findTotemInHotbarForOffhand(player, config);
                    if (hotbarSlot != -1) {
                        beginHotbarToOffhand(player, hotbarSlot, config);
                        totemJustPopped = false;
                        lastOffhandWasTotem = isTotem(player.getOffhandItem());
                        lastTrackedSlot = getSlot(player);
                        return;
                    }

                    int invSlot = findTotemInMainInventory(player);
                    if (invSlot != -1) {
                        beginInventoryToOffhand(client, player, invSlot);
                        totemJustPopped = false;
                        lastOffhandWasTotem = isTotem(player.getOffhandItem());
                        return;
                    }
                }
            }
        }

        // === HOTBAR RESTOCK (both mode) ===
        if (mode.equals("both")) {
            int prefSlot = config.autoTotemHotbarSlot;
            if (!isTotem(player.getInventory().getItem(prefSlot))) {
                int invSlot = findTotemInMainInventory(player);
                if (invSlot != -1) {
                    beginInventoryToHotbar(client, player, invSlot, prefSlot);
                    lastOffhandWasTotem = isTotem(player.getOffhandItem());
                    return;
                }
            }
        }

        if (offhandIsTotem) {
            totemJustPopped = false;
        }
        lastOffhandWasTotem = offhandIsTotem;
    }

    private void handleState(Minecraft client, LocalPlayer player, PvPConfig config) {
        switch (state) {
            case HOTBAR_SWITCHED -> {
                player.connection.send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                    BlockPos.ZERO, Direction.DOWN
                ));
                state = State.HOTBAR_SWAPPED;
            }

            case HOTBAR_SWAPPED -> {
                if (originalSlot >= 0 && originalSlot < 9) {
                    setSlot(player, originalSlot);
                    lastTrackedSlot = originalSlot;
                }
                originalSlot = -1;
                state = State.IDLE;
                swapInFlight = true;
                swapInFlightTimeout = 20;
                tickDelay = config.autoTotemDelay;
                scheduleSyncCheck();
            }
        }
    }

    private void beginHotbarToOffhand(LocalPlayer player, int hotbarSlot, PvPConfig config) {
        originalSlot = getSlot(player);

        if (originalSlot == hotbarSlot) {
            player.connection.send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                BlockPos.ZERO, Direction.DOWN
            ));
            originalSlot = -1;
            swapInFlight = true;
            swapInFlightTimeout = 20;
            tickDelay = config.autoTotemDelay;
            scheduleSyncCheck();
            return;
        }

        setSlot(player, hotbarSlot);
        state = State.HOTBAR_SWITCHED;
    }

    private void beginInventoryToOffhand(Minecraft client, LocalPlayer player, int invSlot) {
        int containerSlot = toContainerSlot(invSlot);
        client.gameMode.handleInventoryMouseClick(
            player.inventoryMenu.containerId,
            containerSlot, 40, ClickType.SWAP, player
        );

        swapInFlight = true;
        swapInFlightTimeout = 20;
        tickDelay = PvPConfig.getInstance().autoTotemDelay;
        scheduleSyncCheck();
    }

    private void beginInventoryToHotbar(Minecraft client, LocalPlayer player, int invSlot, int hotbarSlot) {
        int containerSlot = toContainerSlot(invSlot);
        client.gameMode.handleInventoryMouseClick(
            player.inventoryMenu.containerId,
            containerSlot, hotbarSlot, ClickType.SWAP, player
        );

        swapInFlight = true;
        swapInFlightTimeout = 20;
        tickDelay = PvPConfig.getInstance().autoTotemDelay;
        scheduleSyncCheck();
    }

    private void antiGhostTotem(Minecraft client, LocalPlayer player) {
        if (!PvPConfig.getInstance().autoTotemAntiGhost) return;

        if (syncCountdown > 0) {
            syncCountdown--;
            if (syncCountdown == 0) {
                if (isTotem(player.getOffhandItem())) {
                    int containerId = player.inventoryMenu.containerId;
                    client.gameMode.handleInventoryMouseClick(containerId, 45, 0, ClickType.PICKUP, player);
                    client.gameMode.handleInventoryMouseClick(containerId, 45, 0, ClickType.PICKUP, player);
                }
            }
        }
    }

    private void scheduleSyncCheck() {
        if (PvPConfig.getInstance().autoTotemAntiGhost) {
            syncCountdown = 3;
        }
    }

    private boolean shouldForceTotem(LocalPlayer player, PvPConfig config) {
        float effectiveHealth = player.getHealth() + player.getAbsorptionAmount();

        if (effectiveHealth <= config.autoTotemHealthThreshold) return true;
        if (config.autoTotemElytra && player.isFallFlying()) return true;

        if (config.autoTotemFall && player.fallDistance > 3.0) {
            double fallDamage = player.fallDistance - 3.0;
            if (fallDamage >= effectiveHealth) return true;
        }

        return false;
    }

    private void resetAll(Minecraft client) {
        if ((state == State.HOTBAR_SWITCHED || state == State.HOTBAR_SWAPPED)
                && originalSlot >= 0 && originalSlot < 9 && client.player != null) {
            setSlot(client.player, originalSlot);
        }

        state = State.IDLE;
        originalSlot = -1;
        tickDelay = 0;
        totemJustPopped = false;
        lastOffhandWasTotem = false;
        playerOverrideCooldown = 0;
        lastTrackedSlot = -1;
        swapInFlight = false;
        swapInFlightTimeout = 0;
        syncCountdown = 0;
    }

    private boolean isTotem(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.TOTEM_OF_UNDYING);
    }

    private int findTotemInHotbar(LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            if (isTotem(player.getInventory().getItem(i))) return i;
        }
        return -1;
    }

    private int findTotemInHotbarForOffhand(LocalPlayer player, PvPConfig config) {
        for (int i = 0; i < 9; i++) {
            if (config.autoTotemMode.equals("both") && i == config.autoTotemHotbarSlot) continue;
            if (isTotem(player.getInventory().getItem(i))) return i;
        }
        return -1;
    }

    private int findTotemInMainInventory(LocalPlayer player) {
        for (int i = 9; i < 36; i++) {
            if (isTotem(player.getInventory().getItem(i))) return i;
        }
        return -1;
    }

    private int toContainerSlot(int invSlot) {
        if (invSlot < 9) return invSlot + 36;
        return invSlot;
    }

    private int getSlot(LocalPlayer player) {
        return player.getInventory().getSelectedSlot();
    }

    private void setSlot(LocalPlayer player, int slot) {
        player.getInventory().setSelectedSlot(slot);
    }
}