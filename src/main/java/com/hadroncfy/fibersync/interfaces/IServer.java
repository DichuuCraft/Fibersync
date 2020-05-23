package com.hadroncfy.fibersync.interfaces;

import com.hadroncfy.fibersync.backup.BackupEntry;
import com.hadroncfy.fibersync.command.BackupCommandContext;

public interface IServer {
    void reloadAll(BackupEntry entry, Runnable callback);
    void setTickTask(Runnable task, int period);
    BackupCommandContext getContext();
}