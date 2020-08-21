package com.hadroncfy.fibersync.util.copy;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class FileDeleter {
    private final List<Path> paths = new ArrayList<>();
    private final Path path;
    private FileOperationProgressListener listener;
    private long totalSize = 0;

    public FileDeleter(Path path) {
        this.path = path;
    }

    public FileDeleter setListener(FileOperationProgressListener listener) {
        this.listener = listener;
        return this;
    }

    public void run() throws IOException {
        Files.walkFileTree(path, new Visitor());
        if (listener != null) {
            listener.start(totalSize);
        }
        for (Path p : paths) {
            final File pf = p.toFile();
            if (pf.isFile() && listener != null) {
                listener.onFileDone(p, pf.length());
            }
            Files.delete(p);
        }
        
        if (listener != null) {
            listener.done();
        }
    }

    private class Visitor implements FileVisitor<Path> {

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            paths.add(dir);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            totalSize += file.toFile().length();
            paths.add(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }
    }
}