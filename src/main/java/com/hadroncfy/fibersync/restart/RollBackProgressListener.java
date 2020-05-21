package com.hadroncfy.fibersync.restart;

import java.nio.file.Path;

import com.hadroncfy.fibersync.util.copy.FileCopyProgressListener;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.BossBar.Color;
import net.minecraft.entity.boss.BossBar.Style;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket.Type;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;

import static com.hadroncfy.fibersync.FibersyncMod.getFormat;

public class RollBackProgressListener implements WorldGenerationProgressListener, FileCopyProgressListener {
    private final Limbo limbo;
    private int fileCount, copied, loadedChunk;
    private static final int SPAWN_CHUNK_COUNT = 21*21;// sbmojang, the spawn radius is hard-coded...
    private final BossBar fileCopyProgressBar = new ServerBossBar(getFormat().fileCopyBarTitle, Color.GREEN, Style.PROGRESS);
    private final BossBar spawnChunkGenProgressBar = new ServerBossBar(getFormat().startRegionBarTitle, Color.GREEN, Style.PROGRESS);
    private boolean stopped = false;

    public RollBackProgressListener(Limbo limbo){
        this.limbo = limbo;
        fileCopyProgressBar.setPercent(0);
        spawnChunkGenProgressBar.setPercent(0);
    }

    void onPlayerConnected(AwaitingPlayer player){
        player.getConnection().send(new BossBarS2CPacket(Type.ADD, fileCopyProgressBar));
        player.getConnection().send(new BossBarS2CPacket(Type.ADD, spawnChunkGenProgressBar));
    }

    @Override
    public void start(int fileCount) {
        this.fileCount = fileCount;
        copied = 0;
    }

    @Override
    public void onFileCopied(Path file) {
        fileCopyProgressBar.setPercent((float)copied++ / (float)fileCount);
        limbo.sendToAll(new BossBarS2CPacket(Type.UPDATE_PCT, fileCopyProgressBar));
    }

    @Override
    public void done() {
        fileCopyProgressBar.setPercent(1);
        limbo.sendToAll(new BossBarS2CPacket(Type.UPDATE_PCT, fileCopyProgressBar));
    }

    @Override
    public void start(ChunkPos spawnPos) {
        loadedChunk = 0;
    }

    @Override
    public void setChunkStatus(ChunkPos pos, ChunkStatus status) {
        if (!stopped && status == ChunkStatus.FULL){
            spawnChunkGenProgressBar.setPercent((float)loadedChunk++ / (float)SPAWN_CHUNK_COUNT);
            limbo.sendToAll(new BossBarS2CPacket(Type.UPDATE_PCT, spawnChunkGenProgressBar));
        }
    }

    @Override
    public void stop() {
        stopped = true;
    }
    
    public void end(){
        stopped = true;
        limbo.sendToAll(new BossBarS2CPacket(Type.REMOVE, fileCopyProgressBar));
        limbo.sendToAll(new BossBarS2CPacket(Type.REMOVE, spawnChunkGenProgressBar));
    }
}