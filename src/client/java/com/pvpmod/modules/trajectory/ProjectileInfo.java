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
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.WindChargeItem;
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
    public final List<Integer> order;

    private static final List<Integer> ORDER_MDG = List.of(0, 1, 2);
    private static final List<Integer> ORDER_GMD = List.of(2, 0, 1);
    private static final List<Integer> ORDER_GDM = List.of(2, 1, 0);

    public ProjectileInfo(double gravity, double drag, Vec3 initialVelocity, Vec3 offset, Vec3 position, boolean hasWaterCollision, double waterDrag, List<Integer> order) {
        this.gravity = gravity;
        this.drag = drag;
        this.initialVelocity = initialVelocity;
        this.offset = offset;
        this.position = position;
        this.hasWaterCollision = hasWaterCollision;
        this.waterDrag = waterDrag;
        this.underwaterGravity = gravity;
        this.order = order;
    }

    public ProjectileInfo(double gravity, double drag, Vec3 initialVelocity, Vec3 offset, Vec3 position, boolean hasWaterCollision, double waterDrag, double underwaterGravity, List<Integer> order) {
        this.gravity = gravity;
        this.drag = drag;
        this.initialVelocity = initialVelocity;
        this.offset = offset;
        this.position = position;
        this.hasWaterCollision = hasWaterCollision;
        this.waterDrag = waterDrag;
        this.underwaterGravity = underwaterGravity;
        this.order = order;
    }

    public static List<ProjectileInfo> getItemsInfo(ItemStack itemStack, Player player, boolean isMainHand) {
        float tickProgress = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);

        List<ProjectileInfo> projectileInfoList = new ArrayList<>();

        double gravity = 0.05;
        double drag = 0.99;
        double waterDrag = 0.6;

        Item item = itemStack.getItem();

        Vec3 position = player.getEyePosition(tickProgress).add(new Vec3(0, -0.10000000149011612, 0));

        if (item instanceof BowItem) {
            int useTicks = player.getTicksUsingItem();
            float pull = BowItem.getPowerForTime(useTicks);

            Vec3 vel = player.getViewVector(tickProgress).scale(3.0 * pull);
            Vec3 offset = new Vec3(0.2, -0.06, 0.2);

            if (pull >= 0.1) projectileInfoList.add(new ProjectileInfo(gravity, drag, vel, offset, position, false, waterDrag, ORDER_MDG));

        } else if (item instanceof CrossbowItem) {
            Vec3 vel = player.getViewVector(tickProgress).scale(3.15);
            Vec3 offset = new Vec3(0, -0.06, 0.03);

            ChargedProjectiles chargedProjectilesComponent = itemStack.get(DataComponents.CHARGED_PROJECTILES);

            if (chargedProjectilesComponent != null) {
                for (ItemStack projectile : chargedProjectilesComponent.getItems()) {
                    if (projectile.is(Items.FIREWORK_ROCKET)) {
                        vel = player.getViewVector(tickProgress).scale(1.6F);
                        gravity = 0;
                        waterDrag = drag;
                    }
                }
            }

            if (CrossbowItem.isCharged(itemStack)) {
                projectileInfoList.add(new ProjectileInfo(gravity, drag, vel, offset, position, false, waterDrag, ORDER_MDG));
                if (hasEnchantment(itemStack, Enchantments.MULTISHOT)) {
                    float angleOffset = 10f;
                    Vec3 vel1 = vel.yRot((float) Math.toRadians(angleOffset));
                    Vec3 vel2 = vel.yRot((float) Math.toRadians(-angleOffset));
                    projectileInfoList.add(new ProjectileInfo(gravity, drag, vel1, offset, position, false, waterDrag, ORDER_MDG));
                    projectileInfoList.add(new ProjectileInfo(gravity, drag, vel2, offset, position, false, waterDrag, ORDER_MDG));
                }
            }

        } else if (item instanceof TridentItem) {
            waterDrag = 0.99;
            int useTicks = player.getTicksUsingItem();

            Vec3 vel = player.getViewVector(tickProgress).scale(TridentItem.PROJECTILE_SHOOT_POWER);
            Vec3 offset = new Vec3(0.2, 0.1, 0.2);

            if (useTicks >= TridentItem.THROW_THRESHOLD_TIME && !hasEnchantment(itemStack, Enchantments.RIPTIDE))
                projectileInfoList.add(new ProjectileInfo(gravity, drag, vel, offset, position, false, waterDrag, ORDER_MDG));

        } else if (item instanceof SnowballItem || item instanceof EggItem || item instanceof EnderpearlItem) {
            waterDrag = 0.8;
            gravity = 0.03;

            Vec3 vel = player.getViewVector(tickProgress).scale(SnowballItem.PROJECTILE_SHOOT_POWER);
            Vec3 offset = new Vec3(0.2, -0.06, 0.2);

            projectileInfoList.add(new ProjectileInfo(gravity, drag, vel, offset, position, false, waterDrag, ORDER_GDM));

        } else if (item instanceof WindChargeItem) {
            gravity = 0;
            drag = 0.95;
            waterDrag = 0.8;

            Vec3 vel = player.getViewVector(tickProgress);
            Vec3 offset = new Vec3(0.2, -0.06, 0.2);

            projectileInfoList.add(new ProjectileInfo(gravity, drag, vel, offset, position, false, waterDrag, ORDER_MDG));

        } else if (item instanceof ThrowablePotionItem) {
            waterDrag = 0.8;

            Vec3 dir = angleFromRot(player.getXRot(), player.getYRot(), -20.0F);
            Vec3 vel = dir.scale(ThrowablePotionItem.PROJECTILE_SHOOT_POWER);
            Vec3 offset = new Vec3(0.2, -0.06, 0.2);

            projectileInfoList.add(new ProjectileInfo(gravity, drag, vel, offset, position, false, waterDrag, ORDER_GDM));

        } else if (item instanceof ExperienceBottleItem) {
            gravity = 0.07;
            waterDrag = 0.8;

            Vec3 dir = angleFromRot(player.getXRot(), player.getYRot(), -20.0F).normalize();
            Vec3 vel = dir.scale(0.7);
            Vec3 offset = new Vec3(0.2, -0.06, 0.2);

            projectileInfoList.add(new ProjectileInfo(gravity, drag, vel, offset, position, false, waterDrag, ORDER_GDM));

        } else if (item instanceof FishingRodItem && player.fishing == null) {
            float f = player.getXRot();
            float g = player.getYRot();
            float h = Mth.cos(-g * (float) (Math.PI / 180.0) - (float) Math.PI);
            float i = Mth.sin(-g * (float) (Math.PI / 180.0) - (float) Math.PI);
            float j = -Mth.cos(-f * (float) (Math.PI / 180.0));
            float k = Mth.sin(-f * (float) (Math.PI / 180.0));
            Vec3 p = player.getEyePosition(tickProgress);
            position = new Vec3(p.x - i * 0.3, p.y, p.z - h * 0.3);
            Vec3 vec3d = new Vec3(-i, Mth.clamp(-(k / j), -5.0F, 5.0F), -h);
            double m = vec3d.length();
            vec3d = vec3d.multiply(0.6 / m + 0.5, 0.6 / m + 0.5, 0.6 / m + 0.5);
            Vec3 vel = vec3d;

            gravity = 0.03;
            drag = 0.92;

            Vec3 offset = new Vec3(0.16, -0.06, 0.2);

            projectileInfoList.add(new ProjectileInfo(gravity, drag, vel, offset, position, true, drag, ORDER_GMD));
        }

        return projectileInfoList;
    }

    public static boolean hasEnchantment(ItemStack stack, ResourceKey<Enchantment> enchantment) {
        var enchantmentRegistry = Minecraft.getInstance().player.level().registryAccess()
            .lookupOrThrow(Registries.ENCHANTMENT);

        Holder<Enchantment> enchantmentEntry = enchantmentRegistry.getOrThrow(enchantment);

        return EnchantmentHelper.getItemEnchantmentLevel(enchantmentEntry, stack) > 0;
    }

    private static Vec3 angleFromRot(float f, float g, float h) {
        float k = -Mth.sin(g * 0.017453292F) * Mth.cos(f * 0.017453292F);
        float l = -Mth.sin((f + h) * 0.017453292F);
        float m = Mth.cos(g * 0.017453292F) * Mth.cos(f * 0.017453292F);
        return new Vec3(k, l, m).normalize();
    }
}
