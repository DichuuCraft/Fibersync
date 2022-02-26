package com.hadroncfy.fibersync.mixin;

import com.hadroncfy.fibersync.interfaces.IServer;
import com.hadroncfy.fibersync.restart.AwaitingPlayer;
import com.mojang.authlib.GameProfile;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;

@Mixin(ServerLoginNetworkHandler.class)
public class MixinServerLoginNetworkHandler {
    @Shadow @Final MinecraftServer server;
    @Shadow @Final public ClientConnection connection;
    @Shadow GameProfile profile;

    @Inject(method = "acceptPlayer", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/server/PlayerManager;getPlayer(Ljava/util/UUID;)Lnet/minecraft/server/network/ServerPlayerEntity;"
    ), cancellable = true)
    private void onAcceptPlayer(CallbackInfo ci) {
        var limbo = ((IServer) this.server).getLimbo();
        if (limbo != null) {
            limbo.onPlayerConnect(new AwaitingPlayer(limbo, this.profile, this.connection), true);
            ci.cancel();
        }
    }
}
