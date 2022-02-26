package com.hadroncfy.fibersync.backup;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import com.hadroncfy.fibersync.FibersyncMod;
import com.hadroncfy.fibersync.util.FileUtil;
import com.hadroncfy.fibersync.util.copy.FileCopier;
import com.hadroncfy.fibersync.util.copy.FileDeleter;
import com.hadroncfy.fibersync.util.copy.FileOperationProgressListener;

public class BackupEntry implements Comparable<BackupEntry> {
    public static final int FLAG_OVERWORLD = 1;
    public static final int FLAG_NETHER = 2;
    public static final int FLAG_END = 4;

    private static final String WORLDDIR = "world";
    private static final String INFO_JSON = "info.json";

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
        try (Writer writer = Files.newBufferedWriter(this.dir.resolve(INFO_JSON), StandardCharsets.UTF_8)) {
            writer.write(BackupInfo.GSON.toJson(info));
        }
    }

    private boolean checkWorldDir() {
        Path d = dir.resolve(WORLDDIR);
        return d.toFile().isDirectory() && d.resolve("level.dat").toFile().exists() && d.resolve("region").toFile().isDirectory();
    }

    public boolean exists() {
        return dir.resolve(INFO_JSON).toFile().exists() && checkWorldDir();
    }

    public void saveBackup(Path worldDir, FileOperationProgressListener listener) throws IOException,
            NoSuchAlgorithmException {
        if (FibersyncMod.getConfig().removeTargetDirBeforeCopy) {
            new FileDeleter(this.dir).run();
        }
        if (!Files.exists(this.dir)) {
            Files.createDirectories(this.dir);
        }
        Path backupDir = dir.resolve(WORLDDIR);
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
        }

        info.size = new FileCopier(worldDir, backupDir, FibersyncMod.getConfig().fileSkipMode)
            .setExclude(new BackupExcluder(FibersyncMod.getConfig().excludes, 0))
            .setListener(listener)
            .run();
        writeInfo();
    }

    public void delete(FileOperationProgressListener listener) throws IOException {
        new FileDeleter(dir).setListener(listener).run();
    }

    public void overwriteTo(BackupEntry entry) throws IOException {
        Files.move(entry.dir, entry.dir.resolveSibling(this.info.name));
    }

    public void back(Path worldDir, int mask, FileOperationProgressListener listener) throws NoSuchAlgorithmException, IOException {
        new FileCopier(dir.resolve(WORLDDIR), worldDir, FibersyncMod.getConfig().fileSkipMode)
            .setExclude(new BackupExcluder(FibersyncMod.getConfig().excludes, mask))
            .setListener(listener)
            .run();
    }

    @Override
    public int compareTo(BackupEntry o) {
        return o.info.date.compareTo(info.date);
    }

    public boolean collides(BackupEntry other){
        return dir.equals(other.dir);
    }

    public void copyTo(BackupEntry other, FileOperationProgressListener listener)
            throws NoSuchAlgorithmException, IOException {
        other.saveBackup(dir.resolve(WORLDDIR), listener);
    }

    public BackupEntry createAtNewDir(Path dir){
        return new BackupEntry(dir, info);
    }

    public long totalSize() throws IOException {
        if (info.size == 0){
            info.size = FileUtil.totalSize(dir);
            writeInfo();
        }
        return info.size;
    }
}