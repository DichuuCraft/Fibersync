package com.hadroncfy.fibersync.mixin;

import java.io.IOException;
import java.util.Map;
import java.util.function.BooleanSupplier;

import com.hadroncfy.fibersync.command.BackupCommandContext;
import com.hadroncfy.fibersync.interfaces.IServer;
import com.hadroncfy.fibersync.interfaces.IServerChunkManager;
import com.hadroncfy.fibersync.restart.IReloadListener;
import com.hadroncfy.fibersync.restart.Limbo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.nbt.NbtOps;
import net.minecraft.resource.ServerResourceManager;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerNetworkIo;
import net.minecraft.server.ServerTask;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.dynamic.RegistryOps;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelStorage;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer extends ReentrantThreadExecutor<ServerTask> implements IServer {
    public MixinMinecraftServer(String string) {
        super(string);
    }

    @Unique private static final Logger LOGGER = LoggerFactory.getLogger("name");

    @Shadow @Final protected WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory;
    @Shadow @Final protected LevelStorage.Session session;
    @Shadow @Final private ServerNetworkIo networkIo;
    @Shadow @Final protected DynamicRegistryManager.Impl registryManager;
    @Shadow private ServerResourceManager serverResourceManager;
    @Shadow private int ticks;
    @Shadow private volatile boolean running;

    // things that needs reseting
    @Shadow @Final private Map<DimensionType, ServerWorld> worlds;
    @Shadow @Mutable private ServerScoreboard scoreboard;
    @Shadow @Mutable protected SaveProperties saveProperties;

    @Shadow
    public abstract void prepareStartRegion(WorldGenerationProgressListener worldGenerationProgressListener);

    @Shadow
    protected abstract void createWorlds(WorldGenerationProgressListener worldGenerationProgressListener);

    @Shadow
    protected abstract void updateDifficulty(); // setDifficulty

    @Shadow
    protected abstract void loadWorldResourcePack();

    @Unique private BackupCommandContext commandContext = new BackupCommandContext(() -> this.session.getDirectoryName());
    @Unique private Limbo limbo;

    @Unique private IReloadListener reloadCB;
    @Unique private Runnable tickTask;
    @Unique private int tickTaskPeriod;
    @Unique private int tickBase;

    // cannot directly call original loadWorld since other mods might mixin into this method
    @Unique
    private void loadWorld(WorldGenerationProgressListener startRegionListener) {
        this.loadWorldResourcePack();
        createWorlds(startRegionListener);
        this.updateDifficulty();
        prepareStartRegion(startRegionListener);
    }


    @Override
    public void reloadAll(IReloadListener listener){
        if (reloadCB == null) {
            reloadCB = listener;
        } else {
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

            {
                this.tasks.clear();
                this.executionsInProgress = 0;
            }

            final var finished = new boolean[]{false};
            final var reload_thrd = new Thread(() -> {
                reloadCB.onReload(limbo);
                finished[0] = true;
            });
            reload_thrd.start();
            while (!finished[0]) {
                limbo.removeRemovedPlayers();
                this.networkIo.tick();
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            LOGGER.info("Reloading");
            this.resetServer();
            this.loadWorld(limbo.getWorldGenListener());
            limbo.end();

            reloadCB.onReloadDone();
            reloadCB = null;

            for (var world: this.worlds.values()) {
                ((IServerChunkManager) world.getChunkManager()).fsModSetupSpawnInfo();
            }
        }
    }

    @Unique
    private void resetServer() {
        this.scoreboard = new ServerScoreboard((MinecraftServer)(Object)this);
        var registry_ops = RegistryOps.ofLoaded(NbtOps.INSTANCE, this.serverResourceManager.getResourceManager(), this.registryManager);
        var props = this.session.readLevelProperties(registry_ops, this.session.getDataPackSettings());
        if (props != null) {
            this.saveProperties = props;
        } else {
            LOGGER.warn("failed to reload save properties");
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
    public BackupCommandContext getBackupCommandContext() {
        return commandContext;
    }

    @Override
    public Limbo getLimbo() {
        return this.limbo;
    }

    @Override
    public void setLimbo(Limbo limbo) {
        this.limbo = limbo;
    }
}