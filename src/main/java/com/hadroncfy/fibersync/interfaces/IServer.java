package com.hadroncfy.fibersync.interfaces;

import com.hadroncfy.fibersync.backup.BackupEntry;

public interface IServer {
    void reloadAll(BackupEntry entry, Runnable callback);
    void setTickTask(Runnable task, int period);
}