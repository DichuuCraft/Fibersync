package com.hadroncfy.fibersync.mixin;

import java.nio.file.Path;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.storage.LevelStorage;

@Mixin(LevelStorage.class)
public interface LevelStorageAccessor {
    @Accessor("savesDirectory")
    Path getSavesDir();
}