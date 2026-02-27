package com.hadroncfy.fibersync.mixin;

import java.util.Queue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.util.thread.ThreadExecutor;

@Mixin(ThreadExecutor.class)
public interface ThreadExecutorAccessor {
    @Accessor("tasks")
    Queue<Runnable> fibersync$getTasks();

    @Accessor("executionsInProgress")
    int fibersync$getExecutionsInProgress();

    @Accessor("executionsInProgress")
    void fibersync$setExecutionsInProgress(int value);
}
