package com.hadroncfy.fibersync.restart;


public interface IReloadListener {
    void onReload(Limbo limbo);
    void onReloadDone();
}