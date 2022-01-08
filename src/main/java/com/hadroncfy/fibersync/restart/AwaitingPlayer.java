package com.hadroncfy.fibersync.restart;

import com.mojang.authlib.GameProfile;

import net.minecraft.network.ClientConnection;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.dimension.DimensionType;

public class AwaitingPlayer {
    private final ServerPlayerEntity entity;
    private final ClientConnection connection;
    private final ServerDummyPlayHandler handler;
    private final DimensionType dim;
    private boolean removed = false;
    public AwaitingPlayer(Limbo limbo, ServerPlayerEntity entity, ClientConnection connection){
        this.entity = entity;
        this.connection = connection;
        this.dim = entity.getWorld().getDimension();
        entity.world = null;
        handler = new ServerDummyPlayHandler(limbo, this);
    }

    public ServerPlayerEntity getEntity(){
        return entity;
    }

    public DimensionType getDimension(){
        return dim;
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