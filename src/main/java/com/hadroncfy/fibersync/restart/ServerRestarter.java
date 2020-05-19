package com.hadroncfy.fibersync.restart;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import com.hadroncfy.fibersync.backup.BackupEntry;
import com.hadroncfy.fibersync.command.BackupCommand;
import com.hadroncfy.fibersync.config.Config;
import com.hadroncfy.fibersync.config.Formats;
import com.hadroncfy.fibersync.interfaces.IServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.util.concurrent.Promise;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.ServerTask;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class ServerRestarter {
    private static final Logger LOGGER = LogManager.getLogger();
    private final MinecraftServer server;
    private final Supplier<Config> config;
    private final List<AwaitingPlayer> players = new ArrayList<>();
    private volatile boolean running = true;

    public ServerRestarter(MinecraftServer server, Supplier<Config> config) {
        this.server = server;
        this.config = config;
    }

    private Formats getFormat() {
        return config.get().formats;
    }

    public void restart(BackupEntry entry, Runnable then) {
        // final PlayerManager playerManager = server.getPlayerManager();
        // server.send(new ServerTask(server.getTicks(), () -> {
        //     for (ServerPlayerEntity playerEntity: new ArrayList<>(playerManager.getPlayerList())){
        //         AwaitingPlayer aPlayer = new AwaitingPlayer(playerEntity.getGameProfile(), playerEntity.networkHandler.connection);
        //         players.add(aPlayer);
        //         new ServerDummyPlayHandler(aPlayer);
        //         playerManager.remove(playerEntity);
        //     }
        //     server.getPlayerManager().sendToAll(getFormat().rollbackStarted);
        //     ((IServer) server).reloadAll(() -> {

        //     });
        //     ((IServer) server).purgeWorlds();
        //     entry.back(BackupCommand.getWorldDir(server));
        //     ((IServer) server).reloadAll();
        //     server.send(new ServerTask(server.getTicks(), () -> {
        //         for (AwaitingPlayer aPlayer: players){
        //             ServerPlayerEntity playerEntity = playerManager.createPlayer(aPlayer.getProfile());
        //             playerManager.onPlayerConnect(aPlayer.getConnection(), playerEntity);
        //         }
        //         server.getPlayerManager().sendToAll(getFormat().rollbackFinished);
        //     }));
        // }));

        // // This would pause the server
        // server.send(new ServerTask(server.getTicks(), () -> {
            
        //     ticker.cancel();
        //     then.run();
        // }));
        
    }
}