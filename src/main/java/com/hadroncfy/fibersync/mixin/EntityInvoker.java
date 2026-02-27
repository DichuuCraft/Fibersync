package com.hadroncfy.fibersync.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

@Mixin(Entity.class)
public interface EntityInvoker {
    @Invoker("setWorld")
    void fibersync$setWorld(World world);
}
