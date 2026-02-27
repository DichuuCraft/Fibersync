package com.hadroncfy.fibersync.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.authlib.GameProfile;

import net.minecraft.server.network.ServerConfigurationNetworkHandler;

@Mixin(ServerConfigurationNetworkHandler.class)
public interface ServerConfigurationNetworkHandlerAccessor {
    @Accessor("profile")
    GameProfile fibersync$getProfile();
}
