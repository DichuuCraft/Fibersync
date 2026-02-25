package com.hadroncfy.fibersync.mixin;

import java.util.Map;
import java.util.UUID;

import com.hadroncfy.fibersync.interfaces.IPlayerManager;
import com.hadroncfy.fibersync.interfaces.IServer;
import com.hadroncfy.fibersync.interfaces.Unit;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.s2c.play.CommonPlayerSpawnInfo;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.state.NetworkState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.ServerStatHandler;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;

@Mixin(PlayerManager.class)
public class MixinPlayerManager implements IPlayerManager {
    @Unique boolean shouldRefreshScreen;

    @Shadow @Final private MinecraftServer server;
    @Shadow @Final private Map<UUID, ServerStatHandler> statisticsMap;
    @Shadow @Final private Map<UUID, PlayerAdvancementTracker> advancementTrackers;

    @Inject(method = "onPlayerConnect", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V",
        ordinal = 1
    ))
    private void onSendGameJoin(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci){
        if (this.shouldRefreshScreen) {
            ServerWorld currentWorld = (ServerWorld) player.getEntityWorld();
            var dimensionKey = currentWorld.getRegistryKey();
            var dKey = dimensionKey == World.OVERWORLD ? World.NETHER : World.OVERWORLD;
            ServerWorld altWorld = this.server.getWorld(dKey);
            if (altWorld == null) {
                altWorld = currentWorld;
            }

            // Send these two packets to prevent the client from being stuck in the downloading terrain screen
            // https://github.com/VelocityPowered/Velocity/blob/master/proxy/src/main/java/com/velocitypowered/proxy/connection/backend/TransitionSessionHandler.java
            CommonPlayerSpawnInfo altInfo = player.createCommonPlayerSpawnInfo(altWorld);
            CommonPlayerSpawnInfo currentInfo = player.createCommonPlayerSpawnInfo(currentWorld);
            connection.send(new PlayerRespawnS2CPacket(altInfo, (byte) 0));
            connection.send(new PlayerRespawnS2CPacket(currentInfo, (byte) 0));
        }
        var progress_bar = ((IServer) this.server).getBackupCommandContext(null).progress_bar.get();
        if (progress_bar != null) {
            progress_bar.addPlayer(player);
        }
    }

    @Redirect(
        method = "onPlayerConnect",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/ClientConnection;transitionInbound(Lnet/minecraft/network/state/NetworkState;Lnet/minecraft/network/listener/PacketListener;)V"
        )
    )
    private void fibersync$transitionInbound(ClientConnection connection, NetworkState<?> state, PacketListener listener) {
        if (this.shouldRefreshScreen || ((IServer) this.server).getLimbo(null) != null) {
            ClientConnectionAccessor accessor = (ClientConnectionAccessor) (Object) connection;
            accessor.fibersync$setPacketListener(listener);
            accessor.fibersync$setPrePlayStateListener(null);
            return;
        }
        connection.transitionInbound((NetworkState) state, listener);
    }

    @Override
    public void setShouldRefreshScreen(Unit u, boolean bl) {
        shouldRefreshScreen = bl;
    }

    @Override
    public void reset(Unit u) {
        this.statisticsMap.clear();
        this.advancementTrackers.clear();
    }
}
