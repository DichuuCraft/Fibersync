package com.hadroncfy.fibersync.backup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import com.hadroncfy.fibersync.util.copy.FileCopier;
import com.hadroncfy.fibersync.util.copy.FileCopyProgressListener;

public class BackupEntry {
    private final BackupInfo info;
    private final Path dir;

    public BackupEntry(Path dir, BackupInfo info) {
        this.info = info;
        this.dir = dir;
    }

    public BackupInfo getInfo() {
        return info;
    }

    public void writeInfo() throws IOException {
        File infoFile = dir.resolve("info.json").toFile();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(infoFile))) {
            writer.write(BackupInfo.GSON.toJson(info));
        }
    }

    private boolean checkWorldDir() {
        Path d = dir.resolve("world");
        return d.toFile().isDirectory() && d.resolve("level.dat").toFile().exists() && d.resolve("region").toFile().isDirectory();
    }

    public boolean exists() {
        return dir.resolve("info.json").toFile().exists() && checkWorldDir();
    }

    public void doBackup(Path worldDir, FileCopyProgressListener listener) throws IOException,
            NoSuchAlgorithmException {
        File dirFile = dir.toFile();
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        Path backupDir = dir.resolve("world");
        File f = backupDir.toFile();
        if (!f.exists()) {
            f.mkdirs();
        }

        writeInfo();
        // FileUtil.rsync(FibersyncMod.getConfig().rsyncPath, worldDir, backupDir);
        FileCopier.copy(worldDir, backupDir, listener);
    }

    public void back(Path worldDir, FileCopyProgressListener listener) throws NoSuchAlgorithmException, IOException {
        FileCopier.copy(dir.resolve("world"), worldDir, listener);
    }
}