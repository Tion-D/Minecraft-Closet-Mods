package com.pvpmod.mixin;

import com.pvpmod.PvPModClient;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class FreecamCameraMixin {

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "setup", at = @At("RETURN"))
    private void onSetup(Level level, Entity entity, boolean detached, boolean mirrored, float tickDelta, CallbackInfo ci) {
        if (PvPModClient.freecam == null || !PvPModClient.freecam.isActive()) return;

        setPosition(
            PvPModClient.freecam.getX(tickDelta),
            PvPModClient.freecam.getY(tickDelta),
            PvPModClient.freecam.getZ(tickDelta)
        );
        setRotation(
            PvPModClient.freecam.getYaw(tickDelta),
            PvPModClient.freecam.getPitch(tickDelta)
        );
    }

    @Inject(method = "isDetached", at = @At("HEAD"), cancellable = true)
    private void onIsDetached(CallbackInfoReturnable<Boolean> cir) {
        if (PvPModClient.freecam != null && PvPModClient.freecam.isActive()) {
            cir.setReturnValue(true);
        }
    }
}