package com.hadroncfy.fibersync.backup;

import java.util.Date;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class BackupInfo {
    public static final UUID CONSOLE_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .registerTypeAdapter(Date.class, new DateSerializer())
        .registerTypeAdapter(UUID.class, new UUIDSerializer()).create();
    
    public String name;
    public Date date;
    public String description;
    public boolean locked, isOldWorld;
    public UUID creator;

    public void refresh(String description, UUID creator){
        this.date = new Date();
        this.description = description;
        this.creator = creator;
    }
}