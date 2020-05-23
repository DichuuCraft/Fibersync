package com.hadroncfy.fibersync.command;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Supplier;

import com.hadroncfy.fibersync.FibersyncMod;
import com.hadroncfy.fibersync.backup.BackupFactory;

import net.minecraft.server.command.ServerCommandSource;

import static com.hadroncfy.fibersync.FibersyncMod.getFormat;

public class BackupCommandContext implements Supplier<Path> {
    private final BackupFactory bf = new BackupFactory(this);
    private final ConfirmationManager cm = new ConfirmationManager(() -> FibersyncMod.getConfig(), 20000, 1000);;
    private final TaskManager taskmgr = new TaskManager();
    public CountDownTask countDownTask;

    @Override
    public Path get() {
        return new File(FibersyncMod.getConfig().backupDir).toPath();
    }

    public TaskManager getTaskManager(){
        return taskmgr;
    }

    public BackupFactory getBackupFactory(){
        return bf;
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
}