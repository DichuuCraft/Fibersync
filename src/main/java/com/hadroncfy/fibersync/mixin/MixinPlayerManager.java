package com.hadroncfy.fibersync.mixin;

import java.util.Map;
import java.util.UUID;

import com.hadroncfy.fibersync.interfaces.IPlayerManager;
import com.hadroncfy.fibersync.restart.Limbo;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.ServerStatHandler;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.dimension.DimensionType;

@Mixin(PlayerManager.class)
public class MixinPlayerManager implements IPlayerManager {
    private boolean shouldRefreshScreen;
    private Limbo limbo = null;

    @Shadow @Final
    private Map<UUID, ServerStatHandler> statisticsMap;
    @Shadow @Final
    private Map<UUID, PlayerAdvancementTracker> advancementTrackers;

    @Inject(method = "onPlayerConnect", at = @At(
        value = "INVOKE", 
        target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V",
        shift = At.Shift.AFTER,
        ordinal = 0
    ))
    private void onSendGameJoin(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci){
        if (shouldRefreshScreen){
            RegistryKey<World> dimensionKey = player.getServerWorld().getRegistryKey();
            RegistryKey<World> dKey = dimensionKey == World.OVERWORLD ? World.NETHER : World.OVERWORLD;
            DimensionType dType = player.getServerWorld().getDimension();
            GameMode gmode = player.interactionManager.getGameMode();

            // Send these two packets to prevent the client from being stuck in the downloading terrain screen
            // https://github.com/VelocityPowered/Velocity/blob/master/proxy/src/main/java/com/velocitypowered/proxy/connection/backend/TransitionSessionHandler.java
            connection.send(new PlayerRespawnS2CPacket(
                dType,
                dKey,
                0,
                gmode,
                gmode,
                false,
                false,
                true
            ));
            connection.send(new PlayerRespawnS2CPacket(
                dType,
                dimensionKey,
                BiomeAccess.hashSeed(player.getServerWorld().getSeed()),
                gmode,
                gmode,
                player.getServerWorld().isDebugWorld(),
                player.getServerWorld().isFlat(),
                true
            ));
        }
    }

    @Inject(method = "onPlayerConnect", at = @At("HEAD"), cancellable = true)
    private void onPlayerJoin(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci){
        if (limbo != null){
            limbo.onPlayerConnect(player, connection, true);
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

    @Override
    public void reset() {
        statisticsMap.clear();
        advancementTrackers.clear();
    }
}