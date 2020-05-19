package com.hadroncfy.fibersync.restart;

import java.util.ArrayList;
import java.util.List;

import com.hadroncfy.fibersync.FibersyncMod;
import com.hadroncfy.fibersync.interfaces.IPlayerManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.dimension.DimensionType;

public class Limbo implements WorldGenerationProgressListener {
    private static final int SPAWN_RADIUS = 11;
    private static final Logger LOGGER = LogManager.getLogger();
    private final List<AwaitingPlayer> players = new ArrayList<>();
    private final BossBar progressBar = new ServerBossBar(FibersyncMod.getFormat().startRegionBarTitle, BossBar.Color.GREEN, BossBar.Style.PROGRESS);
    private int generatedChunk = 0;
    private final int totalChunk = (SPAWN_RADIUS * 2 + 1) * (SPAWN_RADIUS * 2 + 1);
    private boolean stopped = false;

    public void acceptPlayersFrom(MinecraftServer server) {
        final PlayerManager playerManager = server.getPlayerManager();
        for (ServerPlayerEntity player : new ArrayList<>(playerManager.getPlayerList())) {
            playerManager.remove(player);
            AwaitingPlayer p = new AwaitingPlayer(this, player);
            players.add(p);
        }
    }

    public void sendPlayersBack(MinecraftServer server) {
        final PlayerManager playerManager = server.getPlayerManager();
        final IPlayerManager pm = (IPlayerManager) playerManager;
        final ServerWorld dummy = server.getWorld(DimensionType.OVERWORLD);
        
        sendToAll(new BossBarS2CPacket(BossBarS2CPacket.Type.REMOVE, progressBar));

        pm.setShouldRefreshScreen(true);
        for (AwaitingPlayer player : players) {
            // we can't just create a new ServerPlayerEntity here since the original player may
            // be the instance of an inherited class of ServerPlayerEntity, such as 
            // carpet.patches.EntityPlayerMPFake, or com.hadroncfy.sreplay.recording.Photographer.
            player.getEntity().setWorld(dummy); // avoid NullPointException when loading player data
            player.getEntity().removed = false;
            playerManager.onPlayerConnect(player.getConnection(), player.getEntity());
        }
        pm.setShouldRefreshScreen(false);
    }

    public void sendToAll(Packet<?> packet){
        for (AwaitingPlayer player: players){
            player.getConnection().send(packet);
        }
    }

    void removePlayer(AwaitingPlayer player) {
        players.remove(player);
    }

    public void tick() {

    }

    @Override
    public void start(ChunkPos spawnPos) {
        progressBar.setPercent(0);
        sendToAll(new BossBarS2CPacket(BossBarS2CPacket.Type.ADD, progressBar));
    }

    @Override
    public void setChunkStatus(ChunkPos pos, ChunkStatus status) {
        if (!stopped && status == ChunkStatus.FULL){
            generatedChunk++;
            LOGGER.info("Spawn chunk: {}/{}", generatedChunk, totalChunk);
            progressBar.setPercent((float)generatedChunk / (float)totalChunk);
            sendToAll(new BossBarS2CPacket(BossBarS2CPacket.Type.UPDATE_PCT, progressBar));
        }
    }

    @Override
    public void stop() {
        progressBar.setPercent(1);
        stopped = true;
        sendToAll(new BossBarS2CPacket(BossBarS2CPacket.Type.UPDATE_PCT, progressBar));
    }
}