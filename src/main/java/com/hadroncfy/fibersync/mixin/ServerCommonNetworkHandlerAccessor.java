package com.hadroncfy.fibersync.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerCommonNetworkHandler;

@Mixin(ServerCommonNetworkHandler.class)
public interface ServerCommonNetworkHandlerAccessor {
    @Accessor("server")
    MinecraftServer fibersync$getServer();

    @Accessor("connection")
    ClientConnection fibersync$getConnection();
}
