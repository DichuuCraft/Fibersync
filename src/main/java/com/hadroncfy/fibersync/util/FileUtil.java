package com.hadroncfy.fibersync.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;

public class FileUtil {
    public static byte[] checkSum(MessageDigest md, Path f1) throws IOException {
        try (InputStream is = Files.newInputStream(f1)) {
            byte[] buffer = new byte[8192];
            int rn = is.read(buffer);
            while (rn != -1) {
                md.update(buffer, 0, rn);
                rn = is.read(buffer);
            }

            return md.digest();
        }
    }

    public static long totalSize(Path p){
        long size = 0;
        try {
            final FileSizeVisitor visitor = new FileSizeVisitor();
            Files.walkFileTree(p, visitor);
            size = visitor.size;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return size;
    }

    private static class FileSizeVisitor implements FileVisitor<Path> {
        long size = 0;

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            size += file.toFile().length();
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }

    }
}