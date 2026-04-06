package com.pvpmod.mixin;

import com.pvpmod.PvPModClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class FreecamMouseMixin {

    @Shadow
    private double accumulatedDX;

    @Shadow
    private double accumulatedDY;

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void onTurnPlayer(CallbackInfo ci) {
        if (PvPModClient.freecam == null || !PvPModClient.freecam.isActive()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        double sensitivity = client.options.sensitivity().get() * 0.6 + 0.2;
        double factor = sensitivity * sensitivity * sensitivity * 8.0;

        float dx = (float) (accumulatedDX * factor * 0.15);
        float dy = (float) (accumulatedDY * factor * 0.15);

        accumulatedDX = 0;
        accumulatedDY = 0;

        PvPModClient.freecam.onMouseMoveRaw(dx, dy);

        ci.cancel();
    }
}