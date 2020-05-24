package com.hadroncfy.fibersync.restart;

import com.mojang.authlib.GameProfile;

import net.minecraft.network.ClientConnection;
import net.minecraft.server.network.ServerPlayerEntity;

public class AwaitingPlayer {
    private final ServerPlayerEntity entity;
    private final ClientConnection connection;
    private final ServerDummyPlayHandler handler;
    private boolean removed = false;
    public AwaitingPlayer(Limbo limbo, ServerPlayerEntity entity, ClientConnection connection){
        this.entity = entity;
        this.connection = connection;
        entity.world = null;
        handler = new ServerDummyPlayHandler(limbo, this);
    }

    public ServerPlayerEntity getEntity(){
        return entity;
    }

    public ClientConnection getConnection(){
        return connection;
 
    }

    public boolean isRemoved(){
        return removed;
    }

    public void markAsRemoved(){
        removed = true;
    }
}