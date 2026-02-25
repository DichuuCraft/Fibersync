package com.hadroncfy.fibersync.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;

@Mixin(Scoreboard.class)
public interface ScoreboardAccessor {
    @Accessor("objectives")
    Object2ObjectMap<String, ScoreboardObjective> fibersync$getObjectives();

    @Accessor("objectivesByCriterion")
    Reference2ObjectMap<?, ?> fibersync$getObjectivesByCriterion();

    @Accessor("scores")
    Map<?, ?> fibersync$getScores();

    @Accessor("objectiveSlots")
    Map<?, ?> fibersync$getObjectiveSlots();

    @Accessor("teams")
    Object2ObjectMap<?, ?> fibersync$getTeams();

    @Accessor("teamsByScoreHolder")
    Object2ObjectMap<?, ?> fibersync$getTeamsByScoreHolder();
}
