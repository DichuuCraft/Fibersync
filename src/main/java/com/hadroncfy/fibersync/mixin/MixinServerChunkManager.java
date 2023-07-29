package com.hadroncfy.fibersync.mixin;

import java.util.function.Consumer;

import com.hadroncfy.fibersync.interfaces.IServerChunkManager;
import com.hadroncfy.fibersync.interfaces.Unit;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.SpawnDensityCapper;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.chunk.WorldChunk;

@Mixin(ServerChunkManager.class)
public abstract class MixinServerChunkManager implements IServerChunkManager {
    @Shadow @Final public ThreadedAnvilChunkStorage threadedAnvilChunkStorage;
    @Shadow @Final private ChunkTicketManager ticketManager;
    @Shadow private SpawnHelper.Info spawnInfo;
    @Shadow ServerWorld world;

    @Shadow
    abstract void ifChunkLoaded(long pos, Consumer<WorldChunk> chunkConsumer);

    @Override
    public void setupSpawnInfo(Unit u) {
        this.spawnInfo = SpawnHelper.setupSpawn(this.ticketManager.getTickedChunkCount(), this.world.iterateEntities(), this::ifChunkLoaded, new SpawnDensityCapper(this.threadedAnvilChunkStorage));
    }
}
