package com.hadroncfy.fibersync.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.container.Container;
import net.minecraft.container.ContainerListener;

@Mixin(Container.class)
public interface ContainerAccessor {
    @Accessor("listeners")
    List<ContainerListener> getListeners();
}