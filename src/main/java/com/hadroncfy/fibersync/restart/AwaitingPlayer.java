package com.hadroncfy.fibersync.restart;

import com.mojang.authlib.GameProfile;

import net.minecraft.network.ClientConnection;
import net.minecraft.server.network.ServerPlayerEntity;

public class AwaitingPlayer {
    public final ServerPlayerEntity entity;
    public final ClientConnection connection;
    public final ServerDummyPlayHandler handler;
    public final GameProfile profile;
    public boolean removed = false;
    public AwaitingPlayer(Limbo limbo, ServerPlayerEntity entity, ClientConnection connection){
        this.entity = entity;
        this.connection = connection;
        this.profile = entity.getGameProfile();
        this.handler = new ServerDummyPlayHandler(limbo, this);
    }
    public AwaitingPlayer(Limbo limbo, GameProfile profile, ClientConnection connection){
        this.entity = null;
        this.connection = connection;
        this.profile = profile;
        this.handler = new ServerDummyPlayHandler(limbo, this);
    }
}
