package com.hadroncfy.fibersync.command;

import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;

import com.hadroncfy.fibersync.util.copy.FileOperationProgressListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.BossBar.Color;
import net.minecraft.entity.boss.BossBar.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class FileOperationProgressBar implements FileOperationProgressListener {
    private static final Logger LOGGER = LogManager.getLogger();
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
        float last = (float)this.size / (float)totalSize;
        this.size += size;
        float now = (float)this.size / (float)totalSize;
        progressBar.setPercent(now);
        int i = (int)(last * 10), j = (int)(now * 10);
        if (i != j){
            LOGGER.info("Copying file: {}%", j * 10);
        }
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