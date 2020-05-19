package com.hadroncfy.fibersync.restart;

import com.mojang.authlib.GameProfile;

import net.minecraft.network.ClientConnection;
import net.minecraft.server.network.ServerPlayerEntity;

public class AwaitingPlayer {
    private final ServerPlayerEntity entity;
    private final ClientConnection connection;
    private final ServerDummyPlayHandler handler;
    public AwaitingPlayer(Limbo limbo, ServerPlayerEntity entity){
        this.entity = entity;
        connection = entity.networkHandler.connection;
        entity.world = null;
        entity.networkHandler = null;
        handler = new ServerDummyPlayHandler(limbo, this);
    }

    public ServerPlayerEntity getEntity(){
        return entity;
    }

    public ClientConnection getConnection(){
        return connection;
    }
}