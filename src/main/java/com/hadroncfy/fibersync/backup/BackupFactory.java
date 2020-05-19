package com.hadroncfy.fibersync.backup;

import java.io.File;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import com.google.gson.JsonParseException;

public class BackupFactory {
    private final Supplier<File> dir;
    public BackupFactory(Supplier<File> dir){
        this.dir = dir;
    }

    public BackupEntry getEntry(String name){
        File infoFile = new File(new File(dir.get(), name), "info.json");
        try {
            if (infoFile.exists()){
                try (Reader reader = new InputStreamReader(new FileInputStream(infoFile), StandardCharsets.UTF_8)){
                    return new BackupEntry(dir.get(), BackupInfo.GSON.fromJson(reader, BackupInfo.class));
                }
            }
            else {
                return null;
            }
        }
        catch(IOException | JsonParseException e){
            return null;
        }
    }

    public Collection<BackupEntry> getBackups(){
        List<BackupEntry> ret = new ArrayList<>();
        for (String name: dir.get().list()){
            BackupEntry entry = getEntry(name);
            if (entry != null && entry.exists()){
                ret.add(entry);
            }
        }
        return ret;
    }

    public BackupEntry create(String name, String description, UUID creator){
        BackupInfo info = new BackupInfo();
        info.name = name;
        info.description = description;
        info.date = new Date();
        info.creator = creator;

        File d = new File(dir.get(), name);
        if (!d.exists()){
            d.mkdirs();
        }

        return new BackupEntry(dir.get(), info);
    }
}