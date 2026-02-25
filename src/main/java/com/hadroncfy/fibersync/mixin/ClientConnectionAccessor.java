package com.hadroncfy.fibersync.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import io.netty.channel.Channel;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;

@Mixin(ClientConnection.class)
public interface ClientConnectionAccessor {
    @Accessor("channel")
    Channel fibersync$getChannel();

    @Accessor("packetListener")
    void fibersync$setPacketListener(PacketListener listener);

    @Accessor("prePlayStateListener")
    void fibersync$setPrePlayStateListener(PacketListener listener);
}
