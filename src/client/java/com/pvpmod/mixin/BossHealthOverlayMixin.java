package com.pvpmod.mixin;

import com.pvpmod.config.PvPConfig;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossHealthOverlay.class)
public class BossHealthOverlayMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void noBossBar(CallbackInfo ci) {
        PvPConfig config = PvPConfig.getInstance();
        if (config.noRenderEnabled && config.noBossBar) ci.cancel();
    }
}