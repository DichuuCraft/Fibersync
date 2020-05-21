package com.hadroncfy.fibersync.mixin;

import com.hadroncfy.fibersync.interfaces.IPlayerManager;
import com.hadroncfy.fibersync.restart.Limbo;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.dimension.DimensionType;

@Mixin(PlayerManager.class)
public class MixinPlayerManager implements IPlayerManager {
    private boolean shouldRefreshScreen;
    private Limbo limbo = null;

    @Inject(method = "onPlayerConnect", at = @At(
        value = "INVOKE", 
        target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V",
        shift = At.Shift.AFTER,
        ordinal = 0
    ))
    private void onSendGameJoin(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci){
        if (shouldRefreshScreen){
            DimensionType dType = player.dimension == DimensionType.OVERWORLD ? DimensionType.THE_NETHER : DimensionType.OVERWORLD;
            
            // Send these two packets to prevent the client from being stuck in the downloading terrain screen
            // https://github.com/VelocityPowered/Velocity/blob/master/proxy/src/main/java/com/velocitypowered/proxy/connection/backend/TransitionSessionHandler.java
            connection.send(new PlayerRespawnS2CPacket(dType, player.world.getGeneratorType(), player.interactionManager.getGameMode()));
            connection.send(new PlayerRespawnS2CPacket(player.dimension, player.world.getGeneratorType(), player.interactionManager.getGameMode()));
        }
    }

    @Inject(method = "onPlayerConnect", at = @At("HEAD"), cancellable = true)
    private void onPlayerJoin(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci){
        if (limbo != null){
            limbo.onPlayerConnect(player, connection);
            ci.cancel();
        }
    }

    @Override
    public void setShouldRefreshScreen(boolean bl) {
        shouldRefreshScreen = bl;
    }

    @Override
    public void setLimbo(Limbo limbo) {
        this.limbo = limbo;
    }
}