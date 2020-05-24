package com.hadroncfy.fibersync.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileUtil {
    private static final Logger LOGGER = LogManager.getLogger();

    public static byte[] checkSum(Path f1) throws IOException, NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance("md5");
        try (InputStream is = Files.newInputStream(f1)) {
            DigestInputStream dis = new DigestInputStream(is, md);
            byte[] buffer = new byte[1024];
            int rn;
            do {
                rn = dis.read(buffer);
            } while (rn != -1);

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