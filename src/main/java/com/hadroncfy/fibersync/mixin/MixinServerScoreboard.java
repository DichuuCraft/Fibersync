package com.hadroncfy.fibersync.mixin;

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
    @Shadow @Final private Set<ScoreboardObjective> syncableObjectives;

    @Override
    public void reset(Unit u) {
        ScoreboardAccessor scoreboard = (ScoreboardAccessor) (Object) this;
        scoreboard.fibersync$getObjectives().clear();
        scoreboard.fibersync$getObjectivesByCriterion().clear();
        scoreboard.fibersync$getScores().clear();
        scoreboard.fibersync$getObjectiveSlots().clear();
        scoreboard.fibersync$getTeams().clear();
        scoreboard.fibersync$getTeamsByScoreHolder().clear();
        this.syncableObjectives.clear();
    }
}
