package com.hadroncfy.fibersync.mixin;

import java.util.Map;
import java.util.UUID;

import com.hadroncfy.fibersync.interfaces.IPlayerManager;
import com.hadroncfy.fibersync.interfaces.IServer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.ServerStatHandler;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.dimension.DimensionType;

@Mixin(PlayerManager.class)
public class MixinPlayerManager implements IPlayerManager {
    @Unique boolean shouldRefreshScreen;

    @Shadow @Final private MinecraftServer server;
    @Shadow @Final private Map<UUID, ServerStatHandler> statisticsMap;
    @Shadow @Final private Map<UUID, PlayerAdvancementTracker> advancementTrackers;

    @Inject(method = "onPlayerConnect", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V",
        shift = At.Shift.AFTER,
        ordinal = 0
    ))
    private void onSendGameJoin(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci){
        if (this.shouldRefreshScreen) {
            RegistryKey<World> dimensionKey = player.getWorld().getRegistryKey();
            RegistryKey<World> dKey = dimensionKey == World.OVERWORLD ? World.NETHER : World.OVERWORLD;
            DimensionType dType = player.getWorld().getDimension();
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
                BiomeAccess.hashSeed(player.getWorld().getSeed()),
                gmode,
                gmode,
                player.getWorld().isDebugWorld(),
                player.getWorld().isFlat(),
                true
            ));
        }
        var progress_bar = ((IServer) this.server).getBackupCommandContext().progress_bar.get();
        if (progress_bar != null) {
            progress_bar.addPlayer(player);
        }
    }

    @Override
    public void setShouldRefreshScreen(boolean bl) {
        shouldRefreshScreen = bl;
    }

    @Override
    public void fsModReset() {
        statisticsMap.clear();
        advancementTrackers.clear();
    }
}