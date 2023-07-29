package com.hadroncfy.fibersync.interfaces;

import com.hadroncfy.fibersync.command.BackupCommandContext;
import com.hadroncfy.fibersync.restart.IReloadListener;
import com.hadroncfy.fibersync.restart.Limbo;

public interface IServer {
    void reloadAll(Unit u, IReloadListener listener);
    void setTickTask(Unit u, Runnable task, int period);
    void setLimbo(Unit u, Limbo limbo);
    Limbo getLimbo(Unit u);
    BackupCommandContext getBackupCommandContext(Unit u);
}