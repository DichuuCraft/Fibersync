package com.hadroncfy.fibersync.backup;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import com.google.gson.JsonParseException;

public class BackupFactory {
    private final Supplier<Path> dir;
    public BackupFactory(Supplier<Path> dir){
        this.dir = dir;
    }

    public BackupEntry getEntry(String name){
        final Path p = dir.get().resolve(name);
        var infoFile = p.resolve("info.json");
        try {
            if (Files.exists(infoFile)) {
                try (Reader reader = Files.newBufferedReader(infoFile, StandardCharsets.UTF_8)) {
                    return new BackupEntry(p, BackupInfo.GSON.fromJson(reader, BackupInfo.class));
                }
            } else {
                return null;
            }
        } catch(IOException | JsonParseException e) {
            return null;
        }
    }

    public List<BackupEntry> getBackups(){
        List<BackupEntry> ret = new ArrayList<>();
        final String[] names = dir.get().toFile().list();
        if (names != null){
            for (String name: names){
                BackupEntry entry = getEntry(name);
                if (entry != null && entry.exists()){
                    ret.add(entry);
                }
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

        Path backupEntryDir = dir.get().resolve(name);
        return new BackupEntry(backupEntryDir, info);
    }

    public static int getBackupCount(Collection<BackupEntry> entries){
        int c = 0;
        for (BackupEntry e: entries){
            if (!e.getInfo().isOldWorld){
                c++;
            }
        }
        return c;
    }
}