package com.pvpmod.modules.trajectory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.pvpmod.config.PvPConfig;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;

public class TrajectoryModule {

    private static final Minecraft client = Minecraft.getInstance();

    private static final float[] LINE_COLOR = {1.0f, 1.0f, 1.0f};
    private static final float LINE_ALPHA = 1.0f;

    private static final float[] HIT_COLOR = {1.0f, 0.3f, 0.3f};
    private static final float HIT_ALPHA = 0.4f;

    private static final float[] OUTLINE_COLOR = {1.0f, 0.3f, 0.3f};
    private static final float OUTLINE_ALPHA = 0.8f;

    public void onWorldRender(WorldRenderContext context) {
        PvPConfig config = PvPConfig.getInstance();
        if (!config.trajectoryEnabled) return;

        Player player = client.player;
        if (player == null) return;

        ItemStack itemStack = player.getMainHandItem();
        int handMultiplier = client.options.mainHand().get() == HumanoidArm.RIGHT ? 1 : -1;

        List<ProjectileInfo> projectileInfoList = ProjectileInfo.getItemsInfo(itemStack, player, true);
        if (projectileInfoList.isEmpty()) {
            itemStack = player.getOffhandItem();
            handMultiplier = -handMultiplier;
            projectileInfoList = ProjectileInfo.getItemsInfo(itemStack, player, false);
            if (projectileInfoList.isEmpty()) return;
        }

        showProjectileTrajectory(context, player, projectileInfoList, handMultiplier);
    }

    private void showProjectileTrajectory(WorldRenderContext context, Player player, List<ProjectileInfo> projectileInfoList, int handMultiplier) {
        float tickProgress = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 eye = player.getEyePosition(tickProgress);

        for (ProjectileInfo projectileInfo : projectileInfoList) {
            Vec3 pos = projectileInfo.position == null ? player.getEyePosition() : projectileInfo.position;
            Vec3 handToEyeDelta = getHandToEyeDelta(player, projectileInfo.offset, pos, eye, handMultiplier, tickProgress);

            PreviewImpact previewImpact = calculateTrajectory(pos, player, projectileInfo, true);

            if (previewImpact.impact != null && previewImpact.impact.getType() == HitResult.Type.BLOCK && previewImpact.impact instanceof BlockHitResult blockHitResult) {
                BlockPos impactPos = blockHitResult.getBlockPos();
                RenderUtils.renderFilledBox(context, impactPos.getX(), impactPos.getY(), impactPos.getZ(), impactPos.getX() + 1, impactPos.getY() + 1, impactPos.getZ() + 1, HIT_COLOR, HIT_ALPHA);
                RenderUtils.renderBox(context, impactPos.getX(), impactPos.getY(), impactPos.getZ(), impactPos.getX() + 1, impactPos.getY() + 1, impactPos.getZ() + 1, OUTLINE_COLOR, OUTLINE_ALPHA);
            } else if (previewImpact.entityImpact != null) {
                AABB entityBox = previewImpact.entityImpact.getBoundingBox().inflate(previewImpact.entityImpact.getPickRadius());
                RenderUtils.renderFilledBox(context, entityBox.minX, entityBox.minY, entityBox.minZ, entityBox.maxX, entityBox.maxY, entityBox.maxZ, HIT_COLOR, HIT_ALPHA);
                RenderUtils.renderBox(context, entityBox.minX, entityBox.minY, entityBox.minZ, entityBox.maxX, entityBox.maxY, entityBox.maxZ, OUTLINE_COLOR, OUTLINE_ALPHA);
            }

            int color = 0xFFFFFFFF;
            renderTrajectory(context, previewImpact.trajectoryPoints, handToEyeDelta, color, previewImpact.hasHit);
        }
    }

    private Vec3 getHandToEyeDelta(Player player, Vec3 offset, Vec3 startPos, Vec3 eye, int handMultiplier, float tickProgress) {
        float yaw = (float) Math.toRadians(-player.getViewYRot(tickProgress));
        float pitch = (float) Math.toRadians(-player.getViewXRot(tickProgress));

        Vec3 forward = player.getViewVector(tickProgress);
        Vec3 up = new Vec3(-Math.sin(pitch) * Math.sin(yaw), Math.cos(pitch), -Math.sin(pitch) * Math.cos(yaw)).normalize();
        Vec3 right = forward.cross(up).normalize();

        if (client.gameRenderer.getMainCamera().isDetached()) offset = offset.scale(0);

        return right.scale(handMultiplier * offset.x).add(up.scale(offset.y)).add(forward.scale(offset.z)).add(eye.subtract(startPos));
    }

    private void renderTrajectory(WorldRenderContext context, List<Vec3> trajectoryPoints, Vec3 handToEyeDelta, int color, boolean hasHit) {
        VertexConsumer lineConsumer = context.consumers().getBuffer(RenderTypes.lines());
        Vec3 cam = client.gameRenderer.getMainCamera().position();
        PoseStack matrices = context.matrices();
        matrices.pushPose();
        matrices.translate(-cam.x, -cam.y, -cam.z);

        for (int i = 0; i < trajectoryPoints.size() - 1; i++) {
            Vec3 lerpedDelta = handToEyeDelta.scale((trajectoryPoints.size() - (i * 1.0)) / trajectoryPoints.size());
            Vec3 nextLerpedDelta = handToEyeDelta.scale((trajectoryPoints.size() - (i + 1 * 1.0)) / trajectoryPoints.size());
            Vec3 pos = trajectoryPoints.get(i).add(lerpedDelta);
            Vec3 dir = (trajectoryPoints.get(i + 1).add(nextLerpedDelta)).subtract(pos);
            Vector3f floatPos = new Vector3f((float) pos.x, (float) pos.y, (float) pos.z);

            RenderUtils.renderVector(matrices, lineConsumer, floatPos, dir, color);
        }

        if (hasHit) {
            Vec3 pos = trajectoryPoints.getLast();
            double r = 0.1;
            double x = pos.x;
            double y = pos.y;
            double z = pos.z;

            Vector3f floatPos = new Vector3f((float) (x - r), (float) y, (float) z);
            Vec3 dir = new Vec3(2 * r, 0, 0);
            RenderUtils.renderVector(matrices, lineConsumer, floatPos, dir, color);

            floatPos = new Vector3f((float) x, (float) (y - r), (float) z);
            dir = new Vec3(0, 2 * r, 0);
            RenderUtils.renderVector(matrices, lineConsumer, floatPos, dir, color);

            floatPos = new Vector3f((float) x, (float) y, (float) (z - r));
            dir = new Vec3(0, 0, 2 * r);
            RenderUtils.renderVector(matrices, lineConsumer, floatPos, dir, color);
        }

        matrices.popPose();
    }

    private PreviewImpact calculateTrajectory(Vec3 pos, Player player, ProjectileInfo projectileInfo, boolean canHitEntities) {
        Vec3 prevPos = pos;
        HitResult impact = null;
        Entity entityImpact = null;
        boolean hasHit = false;
        List<Vec3> trajectoryPoints = new ArrayList<>();

        double drag = projectileInfo.drag;
        double gravity = projectileInfo.gravity;

        Vec3 vel = projectileInfo.initialVelocity;

        for (int i = 0; i < 200; i++) {
            trajectoryPoints.add(pos);

            pos = pos.add(vel);

            vel = vel.subtract(0, gravity, 0);

            vel = vel.scale(drag);

            AABB searchBox = new AABB(prevPos, pos).inflate(1.0);

            Entity closest = null;
            double closestDistSq = Double.MAX_VALUE;
            Vec3 entityHitPos = null;

            if (canHitEntities) {
                List<Entity> entities = client.level.getEntitiesOfClass(Entity.class, searchBox,
                        e -> !e.isSpectator() && e.isAlive()
                                && !(e instanceof Projectile) && !(e instanceof ItemEntity)
                                && !(e instanceof ExperienceOrb) && !(e instanceof EnderDragon)
                                && !(e instanceof LocalPlayer));

                for (Entity entity : entities) {
                    AABB entityBox = entity.getBoundingBox().inflate(entity.getPickRadius());
                    Optional<Vec3> hit = entityBox.clip(prevPos, pos);
                    if (hit.isPresent()) {
                        double distSq = prevPos.distanceToSqr(hit.get());
                        if (distSq < closestDistSq) {
                            entityHitPos = hit.get();
                            closest = entity;
                            closestDistSq = distSq;
                        }
                    }
                }
            }

            HitResult blockHit;
            if (projectileInfo.hasWaterCollision) {
                blockHit = player.level().clip(new ClipContext(prevPos, pos, ClipContext.Block.COLLIDER, ClipContext.Fluid.WATER, player));
            } else {
                blockHit = player.level().clip(new ClipContext(prevPos, pos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

                HitResult waterHit = player.level().clip(new ClipContext(prevPos, pos, ClipContext.Block.COLLIDER, ClipContext.Fluid.WATER, player));
                if (blockHit.getType() == HitResult.Type.MISS && waterHit.getType() != HitResult.Type.MISS) {
                    drag = projectileInfo.waterDrag;
                    gravity = projectileInfo.underwaterGravity;
                } else if (blockHit.getType() == HitResult.Type.MISS && waterHit.getType() == HitResult.Type.MISS) {
                    drag = projectileInfo.drag;
                    gravity = projectileInfo.gravity;
                }
            }

            if (blockHit.getType() != HitResult.Type.MISS && prevPos.distanceToSqr(blockHit.getLocation()) < closestDistSq) {
                impact = blockHit;
                pos = blockHit.getLocation();
                trajectoryPoints.add(pos);
                hasHit = true;
                break;
            }

            if (entityHitPos != null) {
                entityImpact = closest;
                pos = entityHitPos;
                trajectoryPoints.add(pos);
                hasHit = true;
                break;
            }

            if (pos.y < player.level().getMinY() - 20) break;

            prevPos = pos;
        }

        return new PreviewImpact(pos, impact, entityImpact, hasHit, trajectoryPoints);
    }
}