package com.pvpmod.modules;

import com.pvpmod.config.PvPConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AutoTotemModule {

    private enum State {
        IDLE,
        HOTBAR_SWITCHED,
        HOTBAR_SWAPPED,
        INV_OPENED,
        INV_CLICKED,
        RESTOCK_OPENED,
        RESTOCK_CLICKED
    }

    private State state = State.IDLE;
    private int originalSlot = -1;
    private int pendingInvSlot = -1;
    private int pendingHotbarTarget = -1;
    private boolean lastOffhandWasTotem = false;
    private boolean totemJustPopped = false;
    private int tickDelay = 0;

    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;

        PvPConfig config = PvPConfig.getInstance();
        if (!config.autoTotemEnabled) {
            resetAll(client);
            return;
        }

        LocalPlayer player = client.player;
        boolean offhandIsTotem = isTotem(player.getOffhandItem());

        if (state == State.IDLE) {
            if (lastOffhandWasTotem && !offhandIsTotem && player.getOffhandItem().isEmpty()) {
                totemJustPopped = true;
            }
        }

        if (state != State.IDLE) {
            handleState(client, player, config);
            lastOffhandWasTotem = isTotem(player.getOffhandItem());
            return;
        }

        if (tickDelay > 0) {
            tickDelay--;
            lastOffhandWasTotem = offhandIsTotem;
            return;
        }

        String mode = config.autoTotemMode;

        if (mode.equals("hotbar") || mode.equals("both")) {
            if (shouldForceTotem(player, config)) {
                if (!isTotem(player.getMainHandItem())) {
                    int totemSlot = findTotemInHotbar(player);
                    if (totemSlot != -1) {
                        setSlot(player, totemSlot);
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
                boolean shouldRestock = false;

                if (mode.equals("both")) {
                    shouldRestock = true;
                } else {
                    if (totemJustPopped) shouldRestock = true;
                    if (!shouldRestock && shouldForceTotem(player, config)) {
                        shouldRestock = true;
                    }
                }

                if (shouldRestock) {
                    int hotbarSlot = findTotemInHotbarForOffhand(player, config);
                    if (hotbarSlot != -1) {
                        beginHotbarToOffhand(player, hotbarSlot);
                        totemJustPopped = false;
                        lastOffhandWasTotem = isTotem(player.getOffhandItem());
                        return;
                    }

                    int invSlot = findTotemInMainInventory(player);
                    if (invSlot != -1 && client.screen == null) {
                        beginInventoryToOffhand(client, player, invSlot);
                        totemJustPopped = false;
                        lastOffhandWasTotem = isTotem(player.getOffhandItem());
                        return;
                    }
                }
            }
        }

        if (mode.equals("both")) {
            int prefSlot = config.autoTotemHotbarSlot;
            if (!isTotem(player.getInventory().getItem(prefSlot))) {
                int invSlot = findTotemInMainInventory(player);
                if (invSlot != -1 && client.screen == null) {
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
                }
                originalSlot = -1;
                state = State.IDLE;
                tickDelay = config.autoTotemDelay;
            }

            case INV_OPENED -> {
                if (!(client.screen instanceof InventoryScreen)) {
                    pendingInvSlot = -1;
                    state = State.IDLE;
                    return;
                }
                int containerSlot = toContainerSlot(pendingInvSlot);
                client.gameMode.handleInventoryMouseClick(
                    player.inventoryMenu.containerId,
                    containerSlot, 40, ClickType.SWAP, player
                );
                state = State.INV_CLICKED;
            }

            case INV_CLICKED -> {
                if (client.screen instanceof InventoryScreen) {
                    client.setScreen(null);
                }
                pendingInvSlot = -1;
                state = State.IDLE;
                tickDelay = config.autoTotemDelay;
            }

            case RESTOCK_OPENED -> {
                if (!(client.screen instanceof InventoryScreen)) {
                    pendingInvSlot = -1;
                    pendingHotbarTarget = -1;
                    state = State.IDLE;
                    return;
                }
                int containerSlot = toContainerSlot(pendingInvSlot);
                client.gameMode.handleInventoryMouseClick(
                    player.inventoryMenu.containerId,
                    containerSlot, pendingHotbarTarget, ClickType.SWAP, player
                );
                state = State.RESTOCK_CLICKED;
            }

            case RESTOCK_CLICKED -> {
                if (client.screen instanceof InventoryScreen) {
                    client.setScreen(null);
                }
                pendingInvSlot = -1;
                pendingHotbarTarget = -1;
                state = State.IDLE;
                tickDelay = config.autoTotemDelay;
            }
        }
    }

    private void beginHotbarToOffhand(LocalPlayer player, int hotbarSlot) {
        originalSlot = getSlot(player);

        if (originalSlot == hotbarSlot) {
            player.connection.send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                BlockPos.ZERO, Direction.DOWN
            ));
            originalSlot = -1;
            state = State.IDLE;
            tickDelay = 1;
            return;
        }

        setSlot(player, hotbarSlot);
        state = State.HOTBAR_SWITCHED;
    }

    private void beginInventoryToOffhand(Minecraft client, LocalPlayer player, int invSlot) {
        pendingInvSlot = invSlot;
        client.setScreen(new InventoryScreen(player));
        state = State.INV_OPENED;
    }

    private void beginInventoryToHotbar(Minecraft client, LocalPlayer player, int invSlot, int hotbarSlot) {
        pendingInvSlot = invSlot;
        pendingHotbarTarget = hotbarSlot;
        client.setScreen(new InventoryScreen(player));
        state = State.RESTOCK_OPENED;
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
        if (state == State.INV_OPENED || state == State.INV_CLICKED
                || state == State.RESTOCK_OPENED || state == State.RESTOCK_CLICKED) {
            if (client.screen instanceof InventoryScreen) {
                client.setScreen(null);
            }
        }
        if ((state == State.HOTBAR_SWITCHED || state == State.HOTBAR_SWAPPED)
                && originalSlot >= 0 && originalSlot < 9 && client.player != null) {
            setSlot(client.player, originalSlot);
        }

        state = State.IDLE;
        originalSlot = -1;
        pendingInvSlot = -1;
        pendingHotbarTarget = -1;
        tickDelay = 0;
        totemJustPopped = false;
        lastOffhandWasTotem = false;
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