package com.pvpmod.mixin;

import com.pvpmod.config.PvPConfig;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.netty.channel.ChannelHandlerContext;

@Mixin(Connection.class)
public class ChannelMixin {

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
            at = @At("HEAD"))
    private void onRead(ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci) {
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V",
            at = @At("HEAD"), cancellable = true)
    private void onSend(Packet<?> packet, CallbackInfo ci) {
        PvPConfig config = PvPConfig.getInstance();
        if (!config.spoofClientBrand) return;

        String packetName = packet.getClass().getSimpleName();

        if (packetName.contains("CustomPayload")) {
            String str = packet.toString();
            if (str.contains("fabric:") || str.contains("c:register") || str.contains("c:version")) {
                ci.cancel();
            }
        }
    }
}