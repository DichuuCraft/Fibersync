package com.hadroncfy.fibersync.backup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import com.hadroncfy.fibersync.util.FileUtil;
import com.hadroncfy.fibersync.util.copy.FileCopier;
import com.hadroncfy.fibersync.util.copy.FileOperationProgressListener;

public class BackupEntry implements Comparable<BackupEntry> {
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
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(infoFile), StandardCharsets.UTF_8)) {
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

    public void doBackup(Path worldDir, FileOperationProgressListener listener) throws IOException,
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
        FileCopier.copy(worldDir, backupDir, listener);
    }

    public void delete() throws IOException {
        FileCopier.deleteFileTree(dir);
    }

    public void overwriteTo(BackupEntry entry){
        File f1 = entry.dir.toFile();
        f1.renameTo(entry.dir.resolveSibling(info.name).toFile());
    }

    public void back(Path worldDir, FileOperationProgressListener listener) throws NoSuchAlgorithmException, IOException {
        FileCopier.copy(dir.resolve("world"), worldDir, listener);
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
        other.doBackup(dir.resolve("world"), listener);
    }

    public BackupEntry createAtNewDir(Path dir){
        return new BackupEntry(dir, info);
    }

    public long totalSize(){
        return FileUtil.totalSize(dir);
    }
}