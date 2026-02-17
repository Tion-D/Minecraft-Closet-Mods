package com.pvpmod.mixin;

import com.pvpmod.config.PvPConfig;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {

    @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
    private static void noFire(CallbackInfo ci) {
        PvPConfig config = PvPConfig.getInstance();
        if (config.noRenderEnabled && config.noFireOverlay) ci.cancel();
    }

    @Inject(method = "renderWater", at = @At("HEAD"), cancellable = true)
    private static void noWater(CallbackInfo ci) {
        PvPConfig config = PvPConfig.getInstance();
        if (config.noRenderEnabled && config.noLiquidOverlay) ci.cancel();
    }
}