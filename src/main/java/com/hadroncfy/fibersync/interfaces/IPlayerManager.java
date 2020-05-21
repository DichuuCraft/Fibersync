package com.hadroncfy.fibersync.interfaces;

import com.hadroncfy.fibersync.restart.Limbo;

public interface IPlayerManager {
    void setShouldRefreshScreen(boolean bl);
    void setLimbo(Limbo limbo);
}