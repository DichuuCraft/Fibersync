package com.hadroncfy.fibersync.restart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.hadroncfy.fibersync.interfaces.IPlayerManager;
import com.hadroncfy.fibersync.util.copy.FileCopyProgressListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.dimension.DimensionType;

public class Limbo implements Runnable {
    private static final int SPAWN_RADIUS = 11;
    private static final Logger LOGGER = LogManager.getLogger();
    private final List<AwaitingPlayer> players = new ArrayList<>();
    private volatile boolean running = false;
    private final Thread ticker = new Thread(this);
    private final MinecraftServer server;
    private final RollBackProgressListener rollBackProgressListener = new RollBackProgressListener(this);

    public Limbo(MinecraftServer server) {
        this.server = server;
    }

    public void start() {
        running = true;
        for (ServerPlayerEntity player : new ArrayList<>(server.getPlayerManager().getPlayerList())) {
            server.getPlayerManager().remove(player);
            onPlayerConnect(player, player.networkHandler.connection);
        }
        ((IPlayerManager)server.getPlayerManager()).setLimbo(this);
        ticker.start();
    }

    public WorldGenerationProgressListener getWorldGenListener(){
        return rollBackProgressListener;
    }

    public FileCopyProgressListener getFileCopyListener(){
        return rollBackProgressListener;
    }

    public void onPlayerConnect(ServerPlayerEntity player, ClientConnection connection){
        AwaitingPlayer p = new AwaitingPlayer(this, player, connection);
        
        rollBackProgressListener.onPlayerConnected(p);
        connection.send(new PlayerPositionLookS2CPacket(0, 0, 0, 0, 0, Collections.emptySet(), 0));
        
        LOGGER.info("Player {} joined limbo", player.getGameProfile().getName());
        addPlayer(p);
    }

    private synchronized void addPlayer(AwaitingPlayer p){
        players.add(p);
    }

    public void end() {
        final PlayerManager playerManager = server.getPlayerManager();
        final IPlayerManager pm = (IPlayerManager) playerManager;
        final ServerWorld dummy = server.getWorld(DimensionType.OVERWORLD);

        running = false;
        try {
            ticker.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        removeRemovedPlayers();

        rollBackProgressListener.end();

        pm.setLimbo(null);
        pm.setShouldRefreshScreen(true);
        for (AwaitingPlayer player : players) {
            // we can't just create a new ServerPlayerEntity here since the original player
            // may
            // be the instance of an inherited class of ServerPlayerEntity, such as
            // carpet.patches.EntityPlayerMPFake, or
            // com.hadroncfy.sreplay.recording.Photographer.
            player.getEntity().setWorld(dummy); // avoid NullPointException when loading player data
            player.getEntity().removed = false;
            playerManager.onPlayerConnect(player.getConnection(), player.getEntity());
        }
        pm.setShouldRefreshScreen(false);
        players.clear();
    }

    public void sendToAll(Packet<?> packet) {
        for (AwaitingPlayer player : players) {
            player.getConnection().send(packet);
        }
    }

    public void broadCast(Text txt){
        sendToAll(new ChatMessageS2CPacket(txt));
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