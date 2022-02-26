package com.hadroncfy.fibersync.restart;

import java.nio.file.Path;

import com.hadroncfy.fibersync.util.copy.FileOperationProgressListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.BossBar.Color;
import net.minecraft.entity.boss.BossBar.Style;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.server.WorldGenerationProgressLogger;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;

import static com.hadroncfy.fibersync.FibersyncMod.getFormat;

public class RollBackProgressListener extends WorldGenerationProgressLogger implements FileOperationProgressListener {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Limbo limbo;
    private int loadedChunk;
    private long totalSize, size;
    private static final int SPAWN_CHUNK_RADIUS = 11;// sbmojang, the spawn radius is hard-coded...
    private static final int SPAWN_CHUNK_COUNT = (2*SPAWN_CHUNK_RADIUS + 1) * (2*SPAWN_CHUNK_RADIUS + 1);
    private final BossBar fileCopyProgressBar = new ServerBossBar(getFormat().fileCopyBarTitle, Color.GREEN, Style.PROGRESS);
    private final BossBar spawnChunkGenProgressBar = new ServerBossBar(getFormat().startRegionBarTitle, Color.GREEN, Style.PROGRESS);
    private boolean stopped = false;

    public RollBackProgressListener(Limbo limbo){
        super(SPAWN_CHUNK_RADIUS);
        this.limbo = limbo;
        fileCopyProgressBar.setPercent(0);
        spawnChunkGenProgressBar.setPercent(0);
    }

    void onPlayerConnected(AwaitingPlayer player){
        player.connection.send(BossBarS2CPacket.add(fileCopyProgressBar));
        player.connection.send(BossBarS2CPacket.add(spawnChunkGenProgressBar));
    }

    @Override
    public void start(long totalSize) {
        this.totalSize = totalSize;
        size = 0;
    }

    @Override
    public void onFileDone(Path file, long size) {
        float last = (float)this.size / (float)totalSize;
        this.size += size;
        float now = (float)this.size / (float)totalSize;
        fileCopyProgressBar.setPercent((float)this.size / (float)totalSize);
        limbo.sendToAll(BossBarS2CPacket.updateProgress(fileCopyProgressBar));
        int i = (int)(last * 10), j = (int)(now * 10);
        if (i != j){
            LOGGER.info("Roll back: {}%", j * 10);
        }
    }

    @Override
    public void done() {
        fileCopyProgressBar.setPercent(1);
        limbo.sendToAll(BossBarS2CPacket.updateProgress(fileCopyProgressBar));
    }

    @Override
    public void start(ChunkPos spawnPos) {
        super.start(spawnPos);
        loadedChunk = 0;
    }

    @Override
    public void setChunkStatus(ChunkPos pos, ChunkStatus status) {
        super.setChunkStatus(pos, status);
        if (!stopped && status == ChunkStatus.FULL){
            spawnChunkGenProgressBar.setPercent((float)loadedChunk++ / (float)SPAWN_CHUNK_COUNT);
            limbo.sendToAll(BossBarS2CPacket.updateProgress(spawnChunkGenProgressBar));
        }
    }

    @Override
    public void stop() {
        super.stop();
        stopped = true;
    }
    
    public void end(){
        stopped = true;
        limbo.sendToAll(BossBarS2CPacket.remove(fileCopyProgressBar.getUuid()));
        limbo.sendToAll(BossBarS2CPacket.remove(spawnChunkGenProgressBar.getUuid()));
    }
}