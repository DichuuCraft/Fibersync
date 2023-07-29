package com.hadroncfy.fibersync.restart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.hadroncfy.fibersync.FibersyncMod;
import com.hadroncfy.fibersync.interfaces.IPlayer;
import com.hadroncfy.fibersync.interfaces.IPlayerManager;
import com.hadroncfy.fibersync.interfaces.IServer;
import com.hadroncfy.fibersync.util.copy.FileOperationProgressListener;

import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionTypes;

public class Limbo {
    public final Set<RegistryKey<World>> world_keys;
    private final List<AwaitingPlayer> players = new ArrayList<>();
    private final MinecraftServer server;
    private final RollBackProgressListener rollBackProgressListener = new RollBackProgressListener(this);

    public Limbo(MinecraftServer server) {
        this.server = server;
        this.world_keys = new HashSet<>();
        for (var k: server.getWorldRegistryKeys()) {
            this.world_keys.add(k);
        }
    }

    public MinecraftServer getServer(){
        return server;
    }

    public void start() {
        for (ServerPlayerEntity player : new ArrayList<>(server.getPlayerManager().getPlayerList())) {
            server.getPlayerManager().remove(player);
            var p = new AwaitingPlayer(this, player, player.networkHandler.connection);
            onPlayerConnect(p, false);
        }
        ((IServer) this.server).setLimbo(null, this);
    }

    public WorldGenerationProgressListener getWorldGenListener(){
        return rollBackProgressListener;
    }

    public FileOperationProgressListener getFileCopyListener(){
        return rollBackProgressListener;
    }

    public void onPlayerConnect(AwaitingPlayer p, boolean sendJoin){
        if (sendJoin) {
            p.connection.send(new GameJoinS2CPacket(
                0,
                false,
                GameMode.SPECTATOR,
                GameMode.SPECTATOR,
                this.world_keys,
                this.server.getRegistryManager(),
                DimensionTypes.OVERWORLD,
                World.OVERWORLD,
                0L,
                20,
                10,
                10,
                false, false, false, false,
                Optional.empty(),
                0
            ));
        }
        PlayerAbilities abilities = new PlayerAbilities();
        abilities.allowFlying = true;
        abilities.allowModifyWorld = false;
        abilities.invulnerable = true;
        abilities.flying = true;
        abilities.creativeMode = false;
        p.connection.send(new PlayerAbilitiesS2CPacket(abilities));
        p.connection.send(new PlayerPositionLookS2CPacket(0, 0, 0, 0, 0, Collections.emptySet(), 0));
        rollBackProgressListener.onPlayerConnected(p);

        FibersyncMod.LOGGER.info("Player {} joined limbo", p.profile.getName());
        addPlayer(p);
    }

    private synchronized void addPlayer(AwaitingPlayer p){
        players.add(p);
    }

    public void end() {
        final PlayerManager playerManager = server.getPlayerManager();
        final IPlayerManager pm = (IPlayerManager) playerManager;
        final ServerWorld dummy = server.getOverworld();

        this.removeRemovedPlayers();

        rollBackProgressListener.end();

        pm.reset(null);
        ((IServer) this.server).setLimbo(null, null);

        if (server.isSingleplayer() && players.isEmpty()){
            FibersyncMod.LOGGER.info("Stopping server as the server has no players");
            // server.stop(true);
        } else {
            pm.setShouldRefreshScreen(null, true);
            for (AwaitingPlayer player : players) {
                // we can't just create a new ServerPlayerEntity here since the original player
                // may
                // be the instance of an inherited class of ServerPlayerEntity, such as
                // carpet.patches.EntityPlayerMPFake, or
                // com.hadroncfy.sreplay.recording.Photographer.
                var playerEntity = player.entity;
                if (playerEntity == null) {
                    playerEntity = playerManager.createPlayer(player.profile);
                }
                playerEntity.setWorld(dummy); // avoid NullPointException when loading player data
                ((IPlayer)playerEntity).reset(null);

                playerManager.onPlayerConnect(player.connection, playerEntity);
            }
            pm.setShouldRefreshScreen(null, false);
        }
        players.clear();
    }

    public void sendToAll(Packet<?> packet) {
        for (AwaitingPlayer player : players) {
            player.connection.send(packet);
        }
    }

    public void broadcast(Text txt){
        sendToAll(new GameMessageS2CPacket(txt, false));
    }

    public void tick() {
        this.removeRemovedPlayers();
        this.server.getNetworkIo().tick();
    }

    public synchronized void removeRemovedPlayers(){
        for (Iterator<AwaitingPlayer> iterator = players.iterator(); iterator.hasNext();){
            AwaitingPlayer p = iterator.next();
            if (p.removed){
                iterator.remove();
                FibersyncMod.LOGGER.info("Player {} left limbo", p.profile.getName());
            }
        }
    }
}