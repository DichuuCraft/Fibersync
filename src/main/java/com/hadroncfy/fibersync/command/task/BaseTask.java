package com.hadroncfy.fibersync.command.task;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import com.hadroncfy.fibersync.FibersyncMod;
import com.hadroncfy.fibersync.backup.BackupEntry;
import com.hadroncfy.fibersync.backup.BackupInfo;
import com.hadroncfy.fibersync.command.BackupCommandContext;
import com.hadroncfy.fibersync.command.FileOperationProgressBar;
import com.hadroncfy.fibersync.interfaces.IServer;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;

import static com.hadroncfy.fibersync.FibersyncMod.getFormat;
import static com.hadroncfy.fibersync.config.TextRenderer.render;

public abstract class BaseTask {
    protected final BackupCommandContext cctx;
    protected final ServerCommandSource src;
    protected final MinecraftServer server;

    protected BaseTask(ServerCommandSource src){
        this.src = src;
        this.server = src.getServer();
        this.cctx = ((IServer)server).getBackupCommandContext(null);
    }
    public abstract int run();

    private static void setAutosave(MinecraftServer server, boolean a) {
        for (ServerWorld w : server.getWorlds()) {
            w.savingDisabled = !a;
        }
    }

    private static boolean getAutosave(MinecraftServer server) {
        for (ServerWorld sw : server.getWorlds()) {
            return !sw.savingDisabled;
        }
        return true;
    }

    public static UUID getSourceUUID(ServerCommandSource src) {
        try {
            return src.getPlayer().getUuid();
        } catch (Exception e) {
            //
            return BackupInfo.CONSOLE_UUID;
        }
    }

    protected CompletableFuture<Void> doBackup(BackupEntry entry){
        final boolean autosave = getAutosave(server);
        setAutosave(server, false);
        server.save(false, true, true);
        server.getPlayerManager().saveAllPlayerData();

        String name = entry.getInfo().name;
        String senderName = src.getName();
        server.getPlayerManager().broadcast(render(getFormat().creatingBackup, senderName, name), false);
        return CompletableFuture.runAsync(() -> {
            final FileOperationProgressBar progressBar = new FileOperationProgressBar(server, render(getFormat().creatingBackupTitle, entry.getInfo().name));
            cctx.progress_bar.set(progressBar);
            try {
                Path worldDir = server.getSavePath(WorldSavePath.ROOT);
                FibersyncMod.LOGGER.info("world dir: {}", worldDir);

                entry.saveBackup(worldDir, progressBar);
                server.getPlayerManager().broadcast(render(getFormat().backupComplete, senderName, name), false);
            } catch (Exception e) {
                e.printStackTrace();
                server.getPlayerManager().broadcast(render(getFormat().backupFailed, senderName, e), false);
                throw new CompletionException(e);
            } finally {
                setAutosave(server, autosave);
                progressBar.done();
                cctx.progress_bar.set(null);
            }
        });
    } 
}