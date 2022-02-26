package com.hadroncfy.fibersync.interfaces;

import com.hadroncfy.fibersync.command.BackupCommandContext;
import com.hadroncfy.fibersync.restart.IReloadListener;
import com.hadroncfy.fibersync.restart.Limbo;

public interface IServer {
    void reloadAll(IReloadListener listener);
    void setTickTask(Runnable task, int period);
    void setLimbo(Limbo limbo);
    Limbo getLimbo();
    BackupCommandContext getBackupCommandContext();
}