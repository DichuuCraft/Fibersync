package com.hadroncfy.fibersync.command;

import java.nio.file.Path;
import java.util.function.Supplier;

import com.hadroncfy.fibersync.FibersyncMod;
import com.hadroncfy.fibersync.backup.BackupFactory;

import net.minecraft.server.command.ServerCommandSource;

import static com.hadroncfy.fibersync.FibersyncMod.getFormat;

public class BackupCommandContext {
    private final BackupFactory bf = new BackupFactory(BackupCommandContext::getBackupPath);
    private final BackupFactory mirrorFactory = new BackupFactory(BackupCommandContext::getMirrorPath);
    private final ConfirmationManager cm = new ConfirmationManager(FibersyncMod::getConfig, 20000, 1000);;
    private final TaskManager taskmgr = new TaskManager();
    public CountDownTask countDownTask;

    private static Path getBackupPath(){
        return FibersyncMod.getConfig().backupDir;
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
}