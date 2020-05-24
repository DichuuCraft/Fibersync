package com.hadroncfy.fibersync.command;

import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;

import com.hadroncfy.fibersync.util.copy.FileOperationProgressListener;

import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.BossBar.Color;
import net.minecraft.entity.boss.BossBar.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class FileOperationProgressBar implements FileOperationProgressListener {
    private long totalSize = 0, size = 0;
    private final ServerBossBar progressBar;

    public FileOperationProgressBar(MinecraftServer server, Text title){
        progressBar = new ServerBossBar(title, Color.GREEN, Style.PROGRESS);
        progressBar.setPercent(0);
        for (ServerPlayerEntity player: server.getPlayerManager().getPlayerList()){
            progressBar.addPlayer(player);
        }
    }

    @Override
    public void start(long totalSize) {
        this.totalSize = totalSize;
        size = 0;
        progressBar.setPercent(0);
    }

    @Override
    public void onFileDone(Path file, long size) {
        this.size += size;
        progressBar.setPercent((float)this.size / (float)totalSize);
    }

    @Override
    public void done() {
        progressBar.setPercent(1);
        new Timer().schedule(new TimerTask(){
            @Override
            public void run() {
                progressBar.clearPlayers();
            }
        }, 1000);
    }
}