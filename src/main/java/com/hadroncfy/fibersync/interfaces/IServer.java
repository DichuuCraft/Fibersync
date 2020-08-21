package com.hadroncfy.fibersync.interfaces;

import com.hadroncfy.fibersync.command.BackupCommandContext;
import com.hadroncfy.fibersync.restart.IReloadListener;

public interface IServer {
    void reloadAll(IReloadListener listener);
    void setTickTask(Runnable task, int period);
    BackupCommandContext getContext();
}