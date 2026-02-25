package com.hadroncfy.fibersync.mixin;

import com.hadroncfy.fibersync.interfaces.IServer;
import com.hadroncfy.fibersync.restart.AwaitingPlayer;
import com.mojang.authlib.GameProfile;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.packet.c2s.config.ReadyC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;

@Mixin(ServerConfigurationNetworkHandler.class)
public class MixinServerLoginNetworkHandler {
    @Inject(method = "onReady", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/network/ClientConnection;transitionOutbound(Lnet/minecraft/network/state/NetworkState;)V",
        shift = At.Shift.AFTER
    ), cancellable = true)
    private void onReady(ReadyC2SPacket packet, CallbackInfo ci) {
        ServerCommonNetworkHandler common = (ServerCommonNetworkHandler) (Object) this;
        ServerCommonNetworkHandlerAccessor access = (ServerCommonNetworkHandlerAccessor) common;
        ServerConfigurationNetworkHandlerAccessor configAccess = (ServerConfigurationNetworkHandlerAccessor) this;
        MinecraftServer server = access.fibersync$getServer();
        var limbo = ((IServer) server).getLimbo(null);
        if (limbo != null) {
            GameProfile profile = configAccess.fibersync$getProfile();
            limbo.onPlayerConnect(new AwaitingPlayer(limbo, profile, access.fibersync$getConnection()), true);
            ci.cancel();
        }
    }
}
