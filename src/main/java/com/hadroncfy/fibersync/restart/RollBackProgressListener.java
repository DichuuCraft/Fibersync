package com.hadroncfy.fibersync.restart;

import java.nio.file.Path;

import com.hadroncfy.fibersync.FibersyncMod;
import com.hadroncfy.fibersync.util.copy.FileOperationProgressListener;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.BossBar.Color;
import net.minecraft.entity.boss.BossBar.Style;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;

import static com.hadroncfy.fibersync.FibersyncMod.getFormat;

public class RollBackProgressListener implements FileOperationProgressListener {
    private final Limbo limbo;
    private long totalSize, size;
    private final BossBar fileCopyProgressBar = new ServerBossBar(getFormat().fileCopyBarTitle, Color.GREEN, Style.PROGRESS);
    private final BossBar spawnChunkGenProgressBar = new ServerBossBar(getFormat().startRegionBarTitle, Color.GREEN, Style.PROGRESS);
    private boolean stopped = false;

    public RollBackProgressListener(Limbo limbo){
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
            FibersyncMod.LOGGER.info("Roll back: {}%", j * 10);
        }
    }

    @Override
    public void done() {
        fileCopyProgressBar.setPercent(1);
        limbo.sendToAll(BossBarS2CPacket.updateProgress(fileCopyProgressBar));
    }

    public void startSpawn() {
        stopped = false;
        spawnChunkGenProgressBar.setPercent(0);
        limbo.sendToAll(BossBarS2CPacket.updateProgress(spawnChunkGenProgressBar));
    }

    public void finishSpawn() {
        stopped = true;
        spawnChunkGenProgressBar.setPercent(1);
        limbo.sendToAll(BossBarS2CPacket.updateProgress(spawnChunkGenProgressBar));
    }
    
    public void end(){
        stopped = true;
        limbo.sendToAll(BossBarS2CPacket.remove(fileCopyProgressBar.getUuid()));
        limbo.sendToAll(BossBarS2CPacket.remove(spawnChunkGenProgressBar.getUuid()));
    }
}
