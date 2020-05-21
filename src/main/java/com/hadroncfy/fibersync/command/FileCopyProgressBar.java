package com.hadroncfy.fibersync.command;

import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;

import com.hadroncfy.fibersync.util.copy.FileCopyProgressListener;

import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.BossBar.Color;
import net.minecraft.entity.boss.BossBar.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import static com.hadroncfy.fibersync.FibersyncMod.getFormat;

public class FileCopyProgressBar implements FileCopyProgressListener {
    private int count, copied = 0;
    private final MinecraftServer server;
    private final ServerBossBar progressBar = new ServerBossBar(getFormat().fileCopyBarTitle, Color.GREEN, Style.PROGRESS);

    public FileCopyProgressBar(MinecraftServer server){
        this.server = server;
        for (ServerPlayerEntity player: server.getPlayerManager().getPlayerList()){
            progressBar.addPlayer(player);
        }
    }

    @Override
    public void start(int fileCount) {
        count = fileCount;
        copied = 0;
        progressBar.setPercent(0);
    }

    @Override
    public void onFileCopied(Path file) {
        progressBar.setPercent((float)copied++ / (float)count);
    }

    @Override
    public void done() {
        progressBar.setPercent(1);
        new Timer().schedule(new TimerTask(){
            @Override
            public void run() {
                progressBar.clearPlayers();
            }
        }, 2000);
    }
}