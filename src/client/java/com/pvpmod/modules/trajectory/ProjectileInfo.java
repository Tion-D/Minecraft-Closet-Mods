package com.pvpmod.modules.trajectory;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;

public class ProjectileInfo {

    public final double gravity;
    public final double drag;
    public final Vec3 initialVelocity;
    public final Vec3 offset;
    public final Vec3 position;
    public final boolean hasWaterCollision;
    public final double waterDrag;
    public final double underwaterGravity;

    public ProjectileInfo(double gravity, double drag, Vec3 initialVelocity, Vec3 offset, Vec3 position,
                          boolean hasWaterCollision, double waterDrag, double underwaterGravity) {
        this.gravity = gravity;
        this.drag = drag;
        this.initialVelocity = initialVelocity;
        this.offset = offset;
        this.position = position;
        this.hasWaterCollision = hasWaterCollision;
        this.waterDrag = waterDrag;
        this.underwaterGravity = underwaterGravity;
    }

    public ProjectileInfo(double gravity, double drag, Vec3 initialVelocity, Vec3 offset, Vec3 position,
                          boolean hasWaterCollision, double waterDrag) {
        this(gravity, drag, initialVelocity, offset, position, hasWaterCollision, waterDrag, gravity);
    }

    public static List<ProjectileInfo> getItemsInfo(ItemStack itemStack, Player player, boolean isMainHand) {
        float tickProgress = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        List<ProjectileInfo> results = new ArrayList<>();
        Item item = itemStack.getItem();

        Vec3 position = player.getEyePosition(tickProgress).add(0, -0.10000000149011612, 0);

        Vec3 playerVel = player.getDeltaMovement();
        Vec3 addedVel = new Vec3(playerVel.x, player.onGround() ? 0 : playerVel.y, playerVel.z);

        if (item instanceof BowItem) {
            int useTicks = player.getTicksUsingItem();
            float pull = BowItem.getPowerForTime(useTicks);
            if (pull < 0.1f) return results;

            Vec3 dir = dirFromRotation(player.getXRot(), player.getYRot(), 0);
            Vec3 vel = dir.normalize().scale(pull * 3.0).add(addedVel);
            Vec3 offset = new Vec3(0.2, -0.06, 0.2);

            results.add(new ProjectileInfo(0.05, 0.99, vel, offset, position, false, 0.6));

        } else if (item instanceof CrossbowItem) {
            if (!CrossbowItem.isCharged(itemStack)) return results;

            double gravity = 0.05;
            double drag = 0.99;
            double waterDrag = 0.6;

            ChargedProjectiles charged = itemStack.get(DataComponents.CHARGED_PROJECTILES);
            boolean isFirework = false;
            if (charged != null) {
                for (ItemStack proj : charged.getItems()) {
                    if (proj.is(Items.FIREWORK_ROCKET)) {
                        isFirework = true;
                        break;
                    }
                }
            }

            float speed = isFirework ? 1.6f : 3.15f;
            if (isFirework) { gravity = 0; waterDrag = drag; }

            Vec3 dir = dirFromRotation(player.getXRot(), player.getYRot(), 0);
            Vec3 vel = dir.normalize().scale(speed).add(addedVel);
            Vec3 offset = new Vec3(0, -0.06, 0.03);

            results.add(new ProjectileInfo(gravity, drag, vel, offset, position, false, waterDrag));

            if (hasEnchantment(itemStack, Enchantments.MULTISHOT) && !isFirework) {
                Vec3 velL = dirFromRotation(player.getXRot(), player.getYRot() - 10, 0).normalize().scale(3.15).add(addedVel);
                Vec3 velR = dirFromRotation(player.getXRot(), player.getYRot() + 10, 0).normalize().scale(3.15).add(addedVel);
                results.add(new ProjectileInfo(gravity, drag, velL, offset, position, false, waterDrag));
                results.add(new ProjectileInfo(gravity, drag, velR, offset, position, false, waterDrag));
            }

        } else if (item instanceof TridentItem) {
            int useTicks = player.getTicksUsingItem();
            if (useTicks < TridentItem.THROW_THRESHOLD_TIME || hasEnchantment(itemStack, Enchantments.RIPTIDE))
                return results;

            Vec3 dir = dirFromRotation(player.getXRot(), player.getYRot(), 0);
            Vec3 vel = dir.normalize().scale(TridentItem.PROJECTILE_SHOOT_POWER).add(addedVel);
            Vec3 offset = new Vec3(0.2, 0.1, 0.2);

            results.add(new ProjectileInfo(0.05, 0.99, vel, offset, position, false, 0.99));

        } else if (item instanceof SnowballItem || item instanceof EggItem || item instanceof EnderpearlItem) {
            Vec3 dir = dirFromRotation(player.getXRot(), player.getYRot(), 0);
            Vec3 vel = dir.normalize().scale(1.5).add(addedVel);
            Vec3 offset = new Vec3(0.2, -0.06, 0.2);

            results.add(new ProjectileInfo(0.03, 0.99, vel, offset, position, false, 0.8));

        } else if (item instanceof WindChargeItem) {
            Vec3 dir = dirFromRotation(player.getXRot(), player.getYRot(), 0);
            Vec3 vel = dir.normalize().add(addedVel);
            Vec3 offset = new Vec3(0.2, -0.06, 0.2);

            results.add(new ProjectileInfo(0, 1.0, vel, offset, position, false, 0.8));

        } else if (item instanceof ThrowablePotionItem) {
            Vec3 dir = dirFromRotation(player.getXRot(), player.getYRot(), -20.0f);
            Vec3 vel = dir.normalize().scale(0.5).add(addedVel);
            Vec3 offset = new Vec3(0.2, -0.06, 0.2);

            results.add(new ProjectileInfo(0.05, 0.99, vel, offset, position, false, 0.8));

        } else if (item instanceof ExperienceBottleItem) {
            Vec3 dir = dirFromRotation(player.getXRot(), player.getYRot(), -20.0f);
            Vec3 vel = dir.normalize().scale(0.7).add(addedVel);
            Vec3 offset = new Vec3(0.2, -0.06, 0.2);

            results.add(new ProjectileInfo(0.07, 0.99, vel, offset, position, false, 0.8));

        } else if (item instanceof FishingRodItem && player.fishing == null) {
            float pitch = player.getXRot();
            float yaw = player.getYRot();
            float h = Mth.cos(-yaw * ((float) Math.PI / 180f) - (float) Math.PI);
            float i = Mth.sin(-yaw * ((float) Math.PI / 180f) - (float) Math.PI);
            float j = -Mth.cos(-pitch * ((float) Math.PI / 180f));
            float k = Mth.sin(-pitch * ((float) Math.PI / 180f));

            Vec3 p = player.getEyePosition(tickProgress);
            position = new Vec3(p.x - (double) i * 0.3, p.y, p.z - (double) h * 0.3);

            Vec3 vec = new Vec3(-i, Mth.clamp(-(k / j), -5.0f, 5.0f), -h);
            double len = vec.length();
            vec = vec.multiply(0.6 / len + 0.5, 0.6 / len + 0.5, 0.6 / len + 0.5);
            Vec3 vel = vec.add(addedVel);

            Vec3 offset = new Vec3(0.16, -0.06, 0.2);
            results.add(new ProjectileInfo(0.03, 0.92, vel, offset, position, true, 0.92));
        }

        return results;
    }

    private static Vec3 dirFromRotation(float pitch, float yaw, float roll) {
        float f = -Mth.sin(yaw * ((float) Math.PI / 180f)) * Mth.cos(pitch * ((float) Math.PI / 180f));
        float g = -Mth.sin((pitch + roll) * ((float) Math.PI / 180f));
        float h = Mth.cos(yaw * ((float) Math.PI / 180f)) * Mth.cos(pitch * ((float) Math.PI / 180f));
        return new Vec3(f, g, h);
    }

    public static boolean hasEnchantment(ItemStack stack, ResourceKey<Enchantment> enchantment) {
        var reg = Minecraft.getInstance().player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> entry = reg.getOrThrow(enchantment);
        return EnchantmentHelper.getItemEnchantmentLevel(entry, stack) > 0;
    }
}