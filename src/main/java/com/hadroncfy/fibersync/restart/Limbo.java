package com.hadroncfy.fibersync.restart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.hadroncfy.fibersync.interfaces.IPlayer;
import com.hadroncfy.fibersync.interfaces.IPlayerManager;
import com.hadroncfy.fibersync.mixin.ContainerAccessor;
import com.hadroncfy.fibersync.util.copy.FileOperationProgressListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.MessageType;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.registry.DynamicRegistryManager.Impl;
import net.minecraft.world.World;

public class Limbo implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger();
    private final List<AwaitingPlayer> players = new ArrayList<>();
    private volatile boolean running = false;
    private final Thread ticker = new Thread(this);
    private final MinecraftServer server;
    private final RollBackProgressListener rollBackProgressListener = new RollBackProgressListener(this);

    public Limbo(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer(){
        return server;
    }

    public void start() {
        running = true;
        for (ServerPlayerEntity player : new ArrayList<>(server.getPlayerManager().getPlayerList())) {
            server.getPlayerManager().remove(player);
            onPlayerConnect(player, player.networkHandler.connection, false);
        }
        ((IPlayerManager)server.getPlayerManager()).setLimbo(this);
        ticker.start();
    }

    public WorldGenerationProgressListener getWorldGenListener(){
        return rollBackProgressListener;
    }

    public FileOperationProgressListener getFileCopyListener(){
        return rollBackProgressListener;
    }

    public void onPlayerConnect(ServerPlayerEntity player, ClientConnection connection, boolean sendJoin){
        AwaitingPlayer p = new AwaitingPlayer(this, player, connection);
        
        if (sendJoin){
            connection.send(new GameJoinS2CPacket(
                player.getEntityId(), 
                player.interactionManager.getGameMode(), 
                player.interactionManager.getPreviousGameMode(),
                0,
                false,
                this.server.getWorldRegistryKeys(),
                (Impl) this.server.getRegistryManager(), 
                this.server.getOverworld().getDimension(),
                World.OVERWORLD,
                20, 
                10, 
                false, false, false, false
            ));
        }
        PlayerAbilities abilities = new PlayerAbilities();
        abilities.allowFlying = true;
        abilities.allowModifyWorld = false;
        abilities.invulnerable = true;
        abilities.flying = true;
        abilities.creativeMode = false;
        connection.send(new PlayerAbilitiesS2CPacket(abilities));
        connection.send(new PlayerPositionLookS2CPacket(0, 0, 0, 0, 0, Collections.emptySet(), 0));
        rollBackProgressListener.onPlayerConnected(p);
        
        LOGGER.info("Player {} joined limbo", player.getGameProfile().getName());
        addPlayer(p);
    }

    private synchronized void addPlayer(AwaitingPlayer p){
        players.add(p);
    }

    public void end() {
        final PlayerManager playerManager = server.getPlayerManager();
        final IPlayerManager pm = (IPlayerManager) playerManager;
        final ServerWorld dummy = server.getOverworld();

        running = false;
        try {
            ticker.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        removeRemovedPlayers();

        rollBackProgressListener.end();

        pm.reset();
        pm.setLimbo(null);

        if (server.isSinglePlayer() && players.isEmpty()){
            LOGGER.info("Stopping server as the server has no players");
            // server.stop(true);
        }
        else {
            pm.setShouldRefreshScreen(true);
            for (AwaitingPlayer player : players) {
                // we can't just create a new ServerPlayerEntity here since the original player
                // may
                // be the instance of an inherited class of ServerPlayerEntity, such as
                // carpet.patches.EntityPlayerMPFake, or
                // com.hadroncfy.sreplay.recording.Photographer.
                final ServerPlayerEntity playerEntity = player.getEntity();
                playerEntity.setWorld(dummy); // avoid NullPointException when loading player data
                playerEntity.removed = false;
                ((IPlayer)playerEntity).reset();
    
                playerManager.onPlayerConnect(player.getConnection(), player.getEntity());
            }
            pm.setShouldRefreshScreen(false);
        }
        players.clear();
    }

    public void sendToAll(Packet<?> packet) {
        for (AwaitingPlayer player : players) {
            player.getConnection().send(packet);
        }
    }

    public void broadcast(Text txt){
        sendToAll(new GameMessageS2CPacket(txt, MessageType.CHAT, new UUID(0, 0)));
    }

    public void tick() {
        removeRemovedPlayers();
        server.getNetworkIo().tick();
    }

    private synchronized void removeRemovedPlayers(){
        for (Iterator<AwaitingPlayer> iterator = players.iterator(); iterator.hasNext();){
            AwaitingPlayer p = iterator.next();
            if (p.isRemoved()){
                iterator.remove();
                LOGGER.info("Player {} left limbo", p.getEntity().getGameProfile().getName());
            }
        }
    }

    @Override
    public void run() {
        while (running) {
            tick();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}