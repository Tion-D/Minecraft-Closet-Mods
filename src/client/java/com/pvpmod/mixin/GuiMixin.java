package com.pvpmod.mixin;

import com.pvpmod.config.PvPConfig;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {

    @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true)
    private void noVignette(GuiGraphics graphics, Entity entity, CallbackInfo ci) {
        PvPConfig config = PvPConfig.getInstance();
        if (config.noRenderEnabled && config.noVignette) ci.cancel();
    }

    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void noScoreboard(CallbackInfo ci) {
        PvPConfig config = PvPConfig.getInstance();
        if (config.noRenderEnabled && config.noScoreboard) ci.cancel();
    }

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void noNauseaOverlay(GuiGraphics graphics, float strength, CallbackInfo ci) {
        PvPConfig config = PvPConfig.getInstance();
        if (config.noRenderEnabled && config.noNausea) ci.cancel();
    }
}