package com.hadroncfy.fibersync.mixin;

import com.hadroncfy.fibersync.interfaces.IServerChunkManager;
import com.hadroncfy.fibersync.interfaces.Unit;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.server.world.ServerChunkManager;

@Mixin(ServerChunkManager.class)
public abstract class MixinServerChunkManager implements IServerChunkManager {
    @Override
    public void setupSpawnInfo(Unit u) {
        // ServerChunkManager now maintains spawn info internally.
    }
}
