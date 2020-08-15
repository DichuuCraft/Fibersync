package com.hadroncfy.fibersync.command;

import static com.hadroncfy.fibersync.FibersyncMod.getFormat;
import static com.hadroncfy.fibersync.config.TextRenderer.render;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import com.hadroncfy.fibersync.FibersyncMod;
import com.hadroncfy.fibersync.backup.BackupEntry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;

public class BackupTask {
    private static final Logger LOGGER = LogManager.getLogger();

    private final BackupCommandContext cctx;
    private final ServerCommandSource src;
    private final MinecraftServer server;

    private BackupEntry selected, overwrite;

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

    public BackupTask(BackupCommandContext cctx, ServerCommandSource src){
        this.cctx = cctx;
        this.src = src;
        this.server = src.getMinecraftServer();
    }

    private CompletableFuture<Void> doBackup(BackupEntry entry){
        final boolean autosave = getAutosave(server);
        setAutosave(server, false);
        server.save(false, true, true);
        server.getPlayerManager().saveAllPlayerData();
        return CompletableFuture.runAsync(() -> {
            final FileOperationProgressBar progressBar = new FileOperationProgressBar(server, render(getFormat().creatingBackupTitle, entry.getInfo().name));
            try {
                Path worldDir = FibersyncMod.getWorldDir(server);
                LOGGER.info("world dir: {}", worldDir);

                entry.doBackup(worldDir, progressBar);
            } catch (Exception e) {
                e.printStackTrace();
                throw new CompletionException(e);
            } finally {
                setAutosave(server, autosave);
                progressBar.done();
            }
        });
    }

    private void runBackupTask(ServerCommandSource dummy){
        String senderName = src.getName();
        if (cctx.tryBeginTask(src)){
            server.getPlayerManager().broadcastChatMessage(render(getFormat().creatingBackup, senderName, name), false);
            try {
                if (overwrite != null && overwrite != selected){
                    selected.overwriteTo(overwrite);
                }
                doBackup(selected).thenRun(() -> {
                    server.getPlayerManager().broadcastChatMessage(render(getFormat().backupComplete, senderName, name), false);
                    cctx.endTask();
                }).exceptionally(e -> {
                    server.getPlayerManager().broadcastChatMessage(render(getFormat().backupFailed, senderName, e), false);
                    cctx.endTask();
                    return null;
                });
            } catch(Exception e){
                e.printStackTrace();
                server.getPlayerManager().broadcastChatMessage(render(getFormat().backupFailed, senderName, e), false);
            } finally {
                cctx.endTask();
            }
        }
    }

    public int back(BackupEntry entry, BackupEntry overwrite){
        this.selected = entry;
        this.overwrite = overwrite;
        if (overwrite != null) {
            if (!overwrite.getInfo().locked){
                src.sendFeedback(render(getFormat().overwriteAlert, overwrite.getInfo().name), false);
                cctx.getConfirmationManager().submit(src.getName(), src, this::runBackupTask);
            }
            else {
                src.sendError(getFormat().overwriteFailedLocked);
                return 0;
            }
        }
        else {
            runBackupTask(src);
        }
        return 1;
    }
}