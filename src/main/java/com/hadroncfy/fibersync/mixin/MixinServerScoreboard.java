package com.hadroncfy.fibersync.mixin;

import java.util.List;
import java.util.Set;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.hadroncfy.fibersync.interfaces.IServerScoreboard;
import com.hadroncfy.fibersync.interfaces.Unit;

import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ServerScoreboard;

@Mixin(ServerScoreboard.class)
public class MixinServerScoreboard implements IServerScoreboard {
    @Shadow @Final private Set<ScoreboardObjective> objectives;
    @Shadow @Final private List<Runnable> updateListeners;

    @Override
    public void reset(Unit u) {
        this.objectives.clear();
        this.updateListeners.clear();
    }
}
