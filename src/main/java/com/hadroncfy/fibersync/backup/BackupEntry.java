package com.hadroncfy.fibersync.backup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.hadroncfy.fibersync.FibersyncMod;
import com.hadroncfy.fibersync.util.FileUtil;

public class BackupEntry {
    private final BackupInfo info;
    private final File dir;

    public BackupEntry(File dir, BackupInfo info){
        this.info = info;
        this.dir = dir;
    }

    public BackupInfo getInfo(){
        return info;
    }

    private File getDir(){
        return new File(dir, info.name);
    }

    public void writeInfo() throws IOException {
        File infoFile = new File(getDir(), "info.json");
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(infoFile))){
            writer.write(BackupInfo.GSON.toJson(info));
        }
    }

    private boolean checkWorldDir(){
        File d = new File(getDir(), "world");
        return new File(d, "level.dat").exists()
        && new File(d, "region").isDirectory();
    }

    public boolean exists(){
        return new File(getDir(), "info.json").exists()
        && new File(getDir(), "world").exists()
        && checkWorldDir();
    }

    public void doBackup(File worldDir) throws IOException {
        File dir = getDir();
        if (!dir.exists()){
            dir.mkdirs();
        }
        File backupDir = new File(dir, "world");
        if (!backupDir.exists()){
            backupDir.mkdirs();
        }
        
        writeInfo();
        FileUtil.rsync(FibersyncMod.getConfig().rsyncPath, worldDir, backupDir);
    }

    public void back(File worldDir){
        File dir = getDir();
        File backupDir = new File(dir, "world");
        FileUtil.rsync(FibersyncMod.getConfig().rsyncPath, backupDir, worldDir);
    }
}