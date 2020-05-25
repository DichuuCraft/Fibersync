package com.hadroncfy.fibersync.backup;

import java.io.File;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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
import com.hadroncfy.fibersync.util.FileUtil;

public class BackupFactory {
    private final Supplier<Path> dir;
    public BackupFactory(Supplier<Path> dir){
        this.dir = dir;
    }

    public BackupEntry getEntry(String levelName, String name){
        final Path p = dir.get().resolve(levelName).resolve(name);
        File infoFile = p.resolve("info.json").toFile();
        try {
            if (infoFile.exists()){
                try (Reader reader = new InputStreamReader(new FileInputStream(infoFile), StandardCharsets.UTF_8)){
                    return new BackupEntry(p, BackupInfo.GSON.fromJson(reader, BackupInfo.class));
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

    public List<BackupEntry> getBackups(String levelName){
        List<BackupEntry> ret = new ArrayList<>();
        final String[] names = dir.get().resolve(levelName).toFile().list();
        if (names != null){
            for (String name: names){
                BackupEntry entry = getEntry(levelName, name);
                if (entry != null && entry.exists()){
                    ret.add(entry);
                }
            }
        }
        return ret;
    }

    public BackupEntry create(String levelName, String name, String description, UUID creator){
        BackupInfo info = new BackupInfo();
        info.name = name;
        info.description = description;
        info.date = new Date();
        info.creator = creator;

        Path backupEntryDir = dir.get().resolve(levelName).resolve(name);
        return new BackupEntry(backupEntryDir, info);
    }

    public long totalSize(){
        return FileUtil.totalSize(dir.get());
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