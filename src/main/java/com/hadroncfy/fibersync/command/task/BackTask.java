package com.hadroncfy.fibersync.command.task;

import com.hadroncfy.fibersync.FibersyncMod;
import com.hadroncfy.fibersync.backup.BackupEntry;
import com.hadroncfy.fibersync.backup.BackupExcluder;
import com.hadroncfy.fibersync.command.FileOperationProgressBar;
import com.hadroncfy.fibersync.config.TextRenderer;
import com.hadroncfy.fibersync.interfaces.IServer;
import com.hadroncfy.fibersync.restart.IReloadListener;
import com.hadroncfy.fibersync.restart.Limbo;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import static com.hadroncfy.fibersync.FibersyncMod.getConfig;
import static com.hadroncfy.fibersync.config.TextRenderer.render;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;

import static com.hadroncfy.fibersync.FibersyncMod.getFormat;

public class BackTask extends BaseTask {
    private final BackupEntry selected;
    private final BackupEntry currentWorld;
    private BackupEntry autoBackup;

    public BackTask(ServerCommandSource src, BackupEntry selected) {
        super(src);
        this.selected = selected;
        currentWorld = cctx.getBackupFactory().create(getConfig().oldWorldName, getConfig().oldWorldDescription,
                getSourceUUID(src));
        currentWorld.getInfo().locked = true;
        currentWorld.getInfo().isOldWorld = true;
    }

    private void startBack(Boolean b) {
        if (b) {
            server.getPlayerManager().broadcast(getStartedText(), false);
            ((IServer) server).reloadAll(null, new ReloadListener());
        } else {
            cctx.endTask();
        }
    }

    private void prepareToBack() {
        cctx.createCountDownTask(i -> {
            Text txt = render(getCountDownTitleText(), Integer.toString(i));
            server.getPlayerManager().broadcast(txt, true);
        }).thenAccept(this::startBack);
    }

    private void runBackTask(ServerCommandSource dummy) {
        if (cctx.tryBeginTask(src)) {
            server.getPlayerManager()
                    .broadcast(render(getStartAlertText(), src.getName(), selected.getInfo().name),  false);

            autoBackup = currentWorld;
            if (selected.collides(currentWorld)) {
                FibersyncMod.LOGGER.info("Backup to temp dir since we are rolling back to oldworld");
                autoBackup = autoBackup.createAtNewDir(getConfig().tempDir);
            }

            doBackup(autoBackup).thenRun(this::prepareToBack).exceptionally(e -> {
                server.getPlayerManager().broadcast(render(getFailedText(), src.getName(), e.toString()),
                    false
                );
                cctx.endTask();
                return null;
            });
        }
    }

    public int run() {
        cctx.getConfirmationManager().submit(src.getName(), src, this::runBackTask);
        return 0;
    }

    protected Text getCountDownTitleText() {
        return getFormat().countDownTitle;
    }

    protected Text getStartAlertText() {
        return getFormat().rollbackConfirmedAlert;
    }

    protected Text getFailedText() {
        return getFormat().backupFailed;
    }

    protected Text getStartedText() {
        return getFormat().rollbackStarted;
    }

    protected Text getFinishedText() {
        return getFormat().rollbackFinished;
    }

    protected int getExcludeMask(){
        return BackupExcluder.MASK_NONE;
    }

    private class ReloadListener implements IReloadListener {
        @Override
        public void onReload(Limbo limbo) {
            try {
                selected.back(server.getSavePath(WorldSavePath.ROOT), getExcludeMask(), limbo.getFileCopyListener());
            } catch (NoSuchAlgorithmException | IOException e) {
                e.printStackTrace();
                limbo.broadcast(TextRenderer.render(FibersyncMod.getFormat().failedToCopyLevelFiles, e.toString()));
            }
        }

        @Override
        public void onReloadDone() {
            server.getPlayerManager().broadcast(getFinishedText(), false);
            if (autoBackup != currentWorld){
                FibersyncMod.LOGGER.info("Copying file back from temp dir");
                CompletableFuture.runAsync(() -> {
                    FileOperationProgressBar progressBar = new FileOperationProgressBar(server, getFormat().fileCopyBarTitle);
                    cctx.progress_bar.set(progressBar);
                    try {
                        autoBackup.copyTo(currentWorld, progressBar);
                        server.getPlayerManager().broadcast(getFormat().copiedFromTempDir, false);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        server.getPlayerManager().broadcast(render(getFormat().failedToCopyFromTempDir, e1.toString()), false);
                    } finally {
                        progressBar.done();
                        cctx.progress_bar.set(null);
                        cctx.endTask();
                    }
                });
            } else {
                cctx.endTask();
            }
        }
    }
}