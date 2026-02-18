package com.pvpmod;

import com.pvpmod.config.PvPConfig;
import com.pvpmod.modules.AimAssistModule;
import com.pvpmod.modules.CriticalsModule;
import com.pvpmod.modules.HitSelectModule;
import com.pvpmod.modules.ShieldDisablerModule;
import com.pvpmod.modules.trajectory.TrajectoryModule;
import com.pvpmod.modules.AutoTotemModule;
import com.pvpmod.modules.NoRenderModule;
import com.pvpmod.modules.PlayerESPModule;
import com.pvpmod.modules.TriggerBotModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;

public class PvPModClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("pvp-mod");

    private final AimAssistModule aimAssist = new AimAssistModule();
    private final CriticalsModule criticals = new CriticalsModule();
    private final ShieldDisablerModule shieldDisabler = new ShieldDisablerModule();
    private final HitSelectModule hitSelect = new HitSelectModule();
    private final TrajectoryModule trajectory = new TrajectoryModule();
    private final AutoTotemModule autoTotem = new AutoTotemModule();
    private final NoRenderModule noRender = new NoRenderModule();
    private final PlayerESPModule playerESP = new PlayerESPModule();
    private final TriggerBotModule triggerBot = new TriggerBotModule();

    private static final String KEY_CATEGORY = "key.categories.pvpmod";
    private static final String KEY_AIM_TOGGLE = "key.pvpmod.aim_toggle";
    private boolean aimKeyHeld = false;
    private boolean aimKeyWasPressed = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("PvP Mod initialized!");

        registerCommands();
        registerEvents();
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("pvp")
                .then(literal("aim")
                    .executes(ctx -> {
                        PvPConfig config = PvPConfig.getInstance();
                        config.aimAssistEnabled = !config.aimAssistEnabled;
                        config.save();
                        ctx.getSource().sendFeedback(Component.literal(
                            "Aim Assist: " + (config.aimAssistEnabled ? "§aON" : "§cOFF")
                        ));
                        return 1;
                    }))

                .then(literal("crit")
                    .executes(ctx -> {
                        PvPConfig config = PvPConfig.getInstance();
                        config.criticalsEnabled = !config.criticalsEnabled;
                        config.save();
                        ctx.getSource().sendFeedback(Component.literal(
                            "Criticals: " + (config.criticalsEnabled ? "§aON" : "§cOFF")
                        ));
                        return 1;
                    })
                    .then(literal("packet")
                        .executes(ctx -> {
                            PvPConfig config = PvPConfig.getInstance();
                            config.criticalsMode = "packet";
                            config.save();
                            ctx.getSource().sendFeedback(Component.literal("§aCrit mode: Packet"));
                            return 1;
                        }))
                    .then(literal("jump")
                        .executes(ctx -> {
                            PvPConfig config = PvPConfig.getInstance();
                            config.criticalsMode = "jump";
                            config.save();
                            ctx.getSource().sendFeedback(Component.literal("§aCrit mode: Jump"));
                            return 1;
                        })))

                .then(literal("shield")
                    .executes(ctx -> {
                        PvPConfig config = PvPConfig.getInstance();
                        config.shieldDisablerEnabled = !config.shieldDisablerEnabled;
                        config.save();
                        ctx.getSource().sendFeedback(Component.literal(
                            "Shield Disabler: " + (config.shieldDisablerEnabled ? "§aON" : "§cOFF")
                        ));
                        return 1;
                    }))

                .then(literal("hitselect")
                    .executes(ctx -> {
                        PvPConfig config = PvPConfig.getInstance();
                        config.hitSelectEnabled = !config.hitSelectEnabled;
                        config.save();
                        ctx.getSource().sendFeedback(Component.literal(
                            "Hit Select: " + (config.hitSelectEnabled ? "§aON" : "§cOFF")
                        ));
                        return 1;
                    }))

                .then(literal("traj")
                    .executes(ctx -> {
                        PvPConfig config = PvPConfig.getInstance();
                        config.trajectoryEnabled = !config.trajectoryEnabled;
                        config.save();
                        ctx.getSource().sendFeedback(Component.literal(
                            "Trajectory: " + (config.trajectoryEnabled ? "§aON" : "§cOFF")
                        ));
                        return 1;
                    }))
                .then(literal("totem")
                    .executes(ctx -> {
                        PvPConfig config = PvPConfig.getInstance();
                        config.autoTotemEnabled = !config.autoTotemEnabled;
                        config.save();
                        ctx.getSource().sendFeedback(Component.literal(
                            "Auto Totem: " + (config.autoTotemEnabled ? "§aON" : "§cOFF")
                        ));
                        return 1;
                    })
                    .then(literal("offhand")
                        .executes(ctx -> {
                            PvPConfig config = PvPConfig.getInstance();
                            config.autoTotemMode = "offhand";
                            config.save();
                            ctx.getSource().sendFeedback(Component.literal("§aTotem mode: Offhand"));
                            return 1;
                        }))
                    .then(literal("hotbar")
                        .executes(ctx -> {
                            PvPConfig config = PvPConfig.getInstance();
                            config.autoTotemMode = "hotbar";
                            config.save();
                            ctx.getSource().sendFeedback(Component.literal("§aTotem mode: Hotbar"));
                            return 1;
                        }))
                    .then(literal("both")
                        .executes(ctx -> {
                            PvPConfig config = PvPConfig.getInstance();
                            config.autoTotemMode = "both";
                            config.save();
                            ctx.getSource().sendFeedback(Component.literal("§aTotem mode: Both (double totem)"));
                            return 1;
                        })))
                .then(literal("render")
                    .executes(ctx -> {
                        PvPConfig config = PvPConfig.getInstance();
                        config.noRenderEnabled = !config.noRenderEnabled;
                        config.save();
                        ctx.getSource().sendFeedback(Component.literal(
                            "No Render: " + (config.noRenderEnabled ? "§aON" : "§cOFF")
                        ));
                        return 1;
                    }))
                .then(literal("esp")
                    .executes(ctx -> {
                        PvPConfig config = PvPConfig.getInstance();
                        config.espEnabled = !config.espEnabled;
                        config.save();
                        ctx.getSource().sendFeedback(Component.literal(
                            "ESP: " + (config.espEnabled ? "§aON" : "§cOFF")
                        ));
                        return 1;
                    }))
                .then(literal("triggerbot")
                    .executes(ctx -> {
                        PvPConfig config = PvPConfig.getInstance();
                        config.triggerBotEnabled = !config.triggerBotEnabled;
                        config.save();
                        ctx.getSource().sendFeedback(Component.literal(
                            "TriggerBot: " + (config.triggerBotEnabled ? "§aON" : "§cOFF")
                        ));
                        return 1;
                    }))
                .then(literal("friend")
                    .then(literal("add")
                        .then(argument("name", StringArgumentType.word())
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "name");
                                PvPConfig config = PvPConfig.getInstance();
                                if (config.addFriend(name)) {
                                    ctx.getSource().sendFeedback(Component.literal("§aAdded §f" + name + "§a to friends"));
                                } else {
                                    ctx.getSource().sendFeedback(Component.literal("§c" + name + " is already a friend"));
                                }
                                return 1;
                            })))
                    .then(literal("remove")
                        .then(argument("name", StringArgumentType.word())
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "name");
                                PvPConfig config = PvPConfig.getInstance();
                                if (config.removeFriend(name)) {
                                    ctx.getSource().sendFeedback(Component.literal("§cRemoved §f" + name + "§c from friends"));
                                } else {
                                    ctx.getSource().sendFeedback(Component.literal("§c" + name + " is not a friend"));
                                }
                                return 1;
                            })))
                    .then(literal("list")
                        .executes(ctx -> {
                            PvPConfig config = PvPConfig.getInstance();
                            if (config.friends.isEmpty()) {
                                ctx.getSource().sendFeedback(Component.literal("§7No friends added"));
                            } else {
                                ctx.getSource().sendFeedback(Component.literal("§aFriends: §f" + String.join(", ", config.friends)));
                            }
                            return 1;
                        }))
                    .then(literal("clear")
                        .executes(ctx -> {
                            PvPConfig config = PvPConfig.getInstance();
                            config.friends.clear();
                            config.save();
                            ctx.getSource().sendFeedback(Component.literal("§cCleared all friends"));
                            return 1;
                        })))

                .then(literal("status")
                    .executes(ctx -> {
                        PvPConfig config = PvPConfig.getInstance();
                        ctx.getSource().sendFeedback(Component.literal(
                            "§6§l--- PvP Mod Status ---\n" +
                            "§7Aim Assist: " + (config.aimAssistEnabled ? "§aON" : "§cOFF") + "\n" +
                            "§7Criticals: " + (config.criticalsEnabled ? "§aON" : "§cOFF") + " §7(" + config.criticalsMode + ")\n" +
                            "§7Shield Disabler: " + (config.shieldDisablerEnabled ? "§aON" : "§cOFF") + "\n" +
                            "§7Hit Select: " + (config.hitSelectEnabled ? "§aON" : "§cOFF") + "\n" +
                            "§7Trajectory: " + (config.trajectoryEnabled ? "§aON" : "§cOFF") + "\n" +
                            "§7Auto Totem: " + (config.autoTotemEnabled ? "§aON" : "§cOFF") + " §7(" + config.autoTotemMode + ")\n" +
                            "§7No Render: " + (config.noRenderEnabled ? "§aON" : "§cOFF") + "\n" +
                            "§7ESO: " + (config.espEnabled ? "§aON" : "§cOFF") + "\n" +
                            "§7Friends: §f" + config.friends.size()
                        ));
                        return 1;
                    }))
            );
        });
    }

    private void registerEvents() {
        ClientPlayConnectionEvents.INIT.register((handler, client) -> {

        });

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            hitSelect.onTickStart(client);
        });

       ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.screen == null && client.player != null) {
                int key = PvPConfig.getInstance().aimToggleKeyCode;
                boolean pressed = InputConstants.isKeyDown(client.getWindow(), key);
                if (pressed && !aimKeyWasPressed) {
                    PvPConfig config = PvPConfig.getInstance();
                    config.aimAssistEnabled = !config.aimAssistEnabled;
                    config.save();
                    client.player.displayClientMessage(
                        Component.literal("Aim Assist: " + (config.aimAssistEnabled ? "§aON" : "§cOFF")),
                        true
                    );
                }
                aimKeyWasPressed = pressed;
            }

            aimAssist.onTick(client);
            shieldDisabler.onTick(client);
            criticals.onTick(client);
            autoTotem.onTick(client);
            noRender.onTick(client);
            triggerBot.onTick(client);
        });

        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            aimAssist.onRender(tickDelta.getGameTimeDeltaPartialTick(true));
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            return criticals.onAttack(player, world, hand, entity, hitResult);
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            trajectory.onWorldRender(context);
            playerESP.onWorldRender(context);
        });
    }
}
