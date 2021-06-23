package com.hadroncfy.fibersync.mixin;

import java.io.IOException;
import java.util.Map;
import java.util.function.BooleanSupplier;

import com.hadroncfy.fibersync.command.BackupCommandContext;
import com.hadroncfy.fibersync.interfaces.IServer;
import com.hadroncfy.fibersync.restart.IReloadListener;
import com.hadroncfy.fibersync.restart.Limbo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.Difficulty;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelStorage;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer implements IServer {
    private static final Logger LOGGER = LogManager.getLogger();
    @Shadow @Final
    protected WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory;

    @Shadow @Final
    private Map<DimensionType, ServerWorld> worlds;

    @Shadow
    public abstract void prepareStartRegion(WorldGenerationProgressListener worldGenerationProgressListener);

    @Shadow
    protected abstract void createWorlds(WorldGenerationProgressListener worldGenerationProgressListener);

    @Shadow
    protected abstract void method_27731(); // setDifficulty

    @Shadow @Final
    protected LevelStorage.Session session;

    @Shadow
    protected abstract void loadWorldResourcePack();

    @Shadow
    private int ticks;

    @Shadow
    private volatile boolean running;

    @Shadow @Mutable
    private ServerScoreboard scoreboard;

    private BackupCommandContext commandContext = new BackupCommandContext(() -> this.session.getDirectoryName());

    // cannot directly call original loadWorld since other mods might mixin into this method
    private void loadWorld(WorldGenerationProgressListener startRegionListener) {
        this.loadWorldResourcePack();
        createWorlds(startRegionListener);
        method_27731();
        prepareStartRegion(startRegionListener);
    }

    private IReloadListener reloadCB;

    private Runnable tickTask;
    private int tickTaskPeriod;
    private int tickBase;

    @Override
    public void reloadAll(IReloadListener listener){
        if (reloadCB == null) {
            reloadCB = listener;
        }
        else {
            throw new IllegalStateException("A reloading task is already in progress!");
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(BooleanSupplier booleanSupplier, CallbackInfo ci) {
        if (reloadCB != null) {
            final Limbo limbo = new Limbo((MinecraftServer) (Object) this);

            limbo.start();
            LOGGER.info("Purging worlds");
            for (ServerWorld world : worlds.values()) {
                try {
                    world.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            worlds.clear();

            reloadCB.onReload(limbo);
            
            LOGGER.info("Reloading");
            scoreboard = new ServerScoreboard((MinecraftServer)(Object)this);
            loadWorld(limbo.getWorldGenListener());
            
            limbo.end();

            reloadCB.onReloadDone();
            reloadCB = null;
        }
    }

    @Inject(method = "tick", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/server/MinecraftServer;tickWorlds(Ljava/util/function/BooleanSupplier;)V"
    ))
    private void onTickWorld(BooleanSupplier booleanSupplier, CallbackInfo ci){
        if (tickTask != null && (ticks - tickBase) % tickTaskPeriod == 0){
            tickTask.run();
        }
    }

    @Override
    public void setTickTask(Runnable task, int period) {
        tickTask = task;
        tickTaskPeriod = period;
        tickBase = ticks;
    }

    @Override
    public BackupCommandContext getContext() {
        return commandContext;
    }
}