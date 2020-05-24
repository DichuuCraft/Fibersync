package com.hadroncfy.fibersync.mixin;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.function.BooleanSupplier;

import com.google.gson.JsonElement;
import com.hadroncfy.fibersync.FibersyncMod;
import com.hadroncfy.fibersync.backup.BackupEntry;
import com.hadroncfy.fibersync.command.BackupCommand;
import com.hadroncfy.fibersync.command.BackupCommandContext;
import com.hadroncfy.fibersync.config.TextRenderer;
import com.hadroncfy.fibersync.interfaces.IServer;
import com.hadroncfy.fibersync.restart.Limbo;
import com.mojang.datafixers.Dynamic;
import com.mojang.datafixers.types.JsonOps;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.datafixer.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.WorldSaveHandler;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.LevelGeneratorType;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;
import net.minecraft.world.level.storage.LevelStorage;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer implements IServer {
    private static final Logger LOGGER = LogManager.getLogger();
    @Shadow @Final
    protected WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory;
    @Shadow @Final
    private String levelName;
    @Shadow @Final
    private LevelStorage levelStorage;
    @Shadow
    private boolean bonusChest;
    @Shadow @Final
    private Map<DimensionType, ServerWorld> worlds;

    @Shadow
    public abstract PlayerManager getPlayerManager();

    @Shadow
    protected abstract void upgradeWorld(String string);

    @Shadow
    protected abstract void createWorlds(WorldSaveHandler worldSaveHandler, LevelProperties properties,
            LevelInfo levelInfo, WorldGenerationProgressListener worldGenerationProgressListener);

    @Shadow
    protected abstract void prepareStartRegion(WorldGenerationProgressListener worldGenerationProgressListener);

    @Shadow
    protected abstract void loadWorldResourcePack(String worldName, WorldSaveHandler worldSaveHandler);

    @Shadow
    public abstract boolean isDemo();

    @Shadow
    public abstract GameMode getDefaultGameMode();

    @Shadow
    public abstract boolean shouldGenerateStructures();

    @Shadow
    public abstract boolean isHardcore();

    @Shadow
    protected abstract void loadWorldDataPacks(File worldDir, LevelProperties levelProperties);

    @Shadow
    public abstract Difficulty getDefaultDifficulty();

    @Shadow
    public abstract void setDifficulty(Difficulty difficulty, boolean bl);

    @Shadow
    private int ticks;

    @Shadow
    private boolean running;

    private BackupCommandContext commandContext = new BackupCommandContext();

    private void loadWorld(String name, String serverName, long seed, LevelGeneratorType generatorType,
            JsonElement generatorSettings, WorldGenerationProgressListener startRegionListener) {
        upgradeWorld(name);
        WorldSaveHandler worldSaveHandler = levelStorage.createSaveHandler(name, (MinecraftServer) (Object) this);
        loadWorldResourcePack(levelName, worldSaveHandler);
        LevelProperties levelProperties = worldSaveHandler.readProperties();
        LevelInfo levelInfo2;
        if (levelProperties == null) {
            if (isDemo()) {
                levelInfo2 = MinecraftServer.DEMO_LEVEL_INFO;
            } else {
                levelInfo2 = new LevelInfo(seed, getDefaultGameMode(), shouldGenerateStructures(), this.isHardcore(),
                        generatorType);
                levelInfo2.setGeneratorOptions(generatorSettings);
                if (bonusChest) {
                    levelInfo2.setBonusChest();
                }
            }

            levelProperties = new LevelProperties(levelInfo2, serverName);
        } else {
            levelProperties.setLevelName(serverName);
            levelInfo2 = new LevelInfo(levelProperties);
        }

        loadWorldDataPacks(worldSaveHandler.getWorldDir(), levelProperties);
        // WorldGenerationProgressListener worldGenerationProgressListener =
        // worldGenerationProgressListenerFactory.create(11);
        createWorlds(worldSaveHandler, levelProperties, levelInfo2, startRegionListener);
        setDifficulty(getDefaultDifficulty(), true);
        prepareStartRegion(startRegionListener);
    }

    private Runnable reloadCB;
    private BackupEntry backupEntry;

    private Runnable tickTask;
    private int tickTaskPeriod;
    private int tickBase;

    @Override
    public void reloadAll(BackupEntry entry, Runnable callback) {
        if (reloadCB == null) {
            reloadCB = callback;
            backupEntry = entry;
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
            final LevelProperties prop = worlds.get(DimensionType.OVERWORLD).getLevelProperties();
            worlds.clear();

            LOGGER.info("Rolling back");
            try {
                backupEntry.back(BackupCommand.getWorldDir((MinecraftServer) (Object) this),
                        limbo.getFileCopyListener());
            } catch (NoSuchAlgorithmException | IOException e) {
                e.printStackTrace();
                limbo.broadcast(TextRenderer.render(FibersyncMod.getFormat().failedToCopyLevelFiles, e.toString()));
            }
            loadWorld(levelName, prop.getLevelName(), prop.getSeed(), prop.getGeneratorType(), Dynamic.convert(NbtOps.INSTANCE, JsonOps.INSTANCE, prop.getGeneratorOptions()), limbo.getWorldGenListener());
            limbo.end();

            reloadCB.run();
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