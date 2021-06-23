package com.hadroncfy.fibersync.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.WorldSaveHandler;

@Mixin(WorldSaveHandler.class)
public interface WorldSaveHandlerAccessor {
    // @Accessor("worldName")
    // String getWorldName();
}