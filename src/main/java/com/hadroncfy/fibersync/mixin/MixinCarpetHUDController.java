package com.hadroncfy.fibersync.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hadroncfy.fibersync.util.CarpetCompat;

@Pseudo
@Mixin(targets = "carpet.logging.HUDController", remap = false)
public class MixinCarpetHUDController {
    @Inject(method = "update_hud", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void fibersync$skipHudIfSpawnerNull(CallbackInfo ci) {
        if (CarpetCompat.isLastSpawnerNull()) {
            ci.cancel();
        }
    }
}
