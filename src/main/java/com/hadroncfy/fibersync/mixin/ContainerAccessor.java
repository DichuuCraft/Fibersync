package com.hadroncfy.fibersync.mixin;

import java.util.List;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ScreenHandler.class)
public interface ContainerAccessor {
    @Accessor("listeners")
    List<ScreenHandlerListener> getListeners();
}