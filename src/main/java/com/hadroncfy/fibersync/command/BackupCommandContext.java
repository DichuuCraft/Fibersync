package com.hadroncfy.fibersync.command;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import com.hadroncfy.fibersync.FibersyncMod;
import com.hadroncfy.fibersync.backup.BackupFactory;

import net.minecraft.server.command.ServerCommandSource;

import static com.hadroncfy.fibersync.FibersyncMod.getFormat;

public class BackupCommandContext {
    private final BackupFactory bf = new BackupFactory(this::getBackupPath);
    private final BackupFactory mirrorFactory = new BackupFactory(BackupCommandContext::getMirrorPath);
    private final ConfirmationManager cm = new ConfirmationManager(FibersyncMod::getFormat, 20000, 1000);;
    private final TaskManager taskmgr = new TaskManager();
    private CountDownTask countDownTask;
    private final Supplier<String> levelName;
    public final AtomicReference<FileOperationProgressBar> progress_bar = new AtomicReference<>();

    public BackupCommandContext(Supplier<String> levelName){
        this.levelName = levelName;
    }

    private Path getBackupPath(){
        return FibersyncMod.getConfig().backupDir.resolve(levelName.get());
    }

    private static Path getMirrorPath(){
        return FibersyncMod.getConfig().syncDir;
    }

    public TaskManager getTaskManager(){
        return taskmgr;
    }

    public BackupFactory getBackupFactory(){
        return bf;
    }

    public BackupFactory getMirrorFactory(){
        return mirrorFactory;
    }

    public ConfirmationManager getConfirmationManager(){
        return cm;
    }

    public boolean tryBeginTask(ServerCommandSource src) {
        if (taskmgr.beginTask()) {
            return true;
        } else {
            src.sendError(getFormat().otherTaskRunning);
            return false;
        }
    }

    public void endTask() {
        taskmgr.endTask();
    }

    public CompletableFuture<Boolean> createCountDownTask(IntConsumer onCountDown){
        countDownTask = new CountDownTask(FibersyncMod.getConfig().defaultCountDown);
        return countDownTask.run(onCountDown).thenApply(b -> {
            countDownTask = null;
            return b;
        });
    }

    public boolean hasCountDownTask(){
        return countDownTask != null;
    }

    public void cancelCountDownTask(){
        countDownTask.cancel();
    }
}