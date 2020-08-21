package com.hadroncfy.fibersync.util.copy;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hadroncfy.fibersync.util.FileUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileCopier {
    private static final Logger LOGGER = LogManager.getLogger("File Copy");
    private final Path src;
    private final Path dest;
    private final List<Path> srcFiles = new ArrayList<>();
    private final List<Path> destFiles = new ArrayList<>();
    private final Set<Path> srcFileSet = new HashSet<>();

    private FileOperationProgressListener listener;
    private PathMatcher exclude;
    private long totalSize = 0;

    public FileCopier(Path src, Path dest) {
        this.src = src;
        this.dest = dest;
    }

    public FileCopier setListener(FileOperationProgressListener listener) {
        this.listener = listener;
        return this;
    }

    public FileCopier setExclude(PathMatcher exclude) {
        this.exclude = exclude;
        return this;
    }

    private static boolean checkSum(Path f1, Path f2) throws IOException, NoSuchAlgorithmException {
        final byte[] b1 = FileUtil.checkSum(f1), b2 = FileUtil.checkSum(f2);
        for (int i = 0; i < b1.length; i++) {
            if (b1[i] != b2[i]) {
                return false;
            }
        }
        return true;
    }

    public long run() throws IOException, NoSuchAlgorithmException {

        Files.walkFileTree(src, new SourceFileVisitor());
        Files.walkFileTree(dest, new DestFileVisitor());

        if (listener != null) {
            listener.start(totalSize);
        }

        for (Path src1 : srcFiles) {
            final Path dest1 = dest.resolve(src1);
            src1 = src.resolve(src1);
            final File src1f = src1.toFile(), dest1f = dest1.toFile();
            if (dest1f.exists()) {
                if (dest1f.isDirectory()) {
                    if (src1f.isFile()) {
                        new FileDeleter(dest1).run();
                    } else {
                        continue;
                    }
                } else {
                    if (src1f.isDirectory()) {
                        Files.delete(dest1);
                    } else if (checkSum(src1, dest1)) {
                        LOGGER.debug("Skipping non-modified file {}", src1);
                        if (listener != null){
                            listener.onFileDone(src1, src1f.length());
                        }
                        continue;
                    }
                }
            }

            Files.copy(src1, dest1, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.debug("Copied file(or dir) {} to {}", src1, dest1);
            
            if (listener != null){
                listener.onFileDone(src1, src1f.length());
            }
        }

        for (Path dest1 : destFiles) {
            if (!srcFileSet.contains(dest1)) {
                Files.delete(dest.resolve(dest1));
                LOGGER.debug("deleted redundant file {}", dest1);
            }
        }

        if (listener != null){
            listener.done();
        }

        return totalSize;
    }

    private class SourceFileVisitor implements FileVisitor<Path> {
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (!dir.equals(src)){
                dir = src.relativize(dir).normalize();
                if (exclude != null && exclude.matches(dir)){
                    return FileVisitResult.SKIP_SUBTREE;
                }
                else {
                    srcFiles.add(dir);
                    srcFileSet.add(dir);
                }
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Path file2 = src.relativize(file).normalize();
            if (exclude == null || !exclude.matches(file2)){
                srcFiles.add(file2);
                srcFileSet.add(file2);
                totalSize += file.toFile().length();
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }
    }

    private class DestFileVisitor implements FileVisitor<Path> {
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            dir = dest.relativize(dir).normalize();
            destFiles.add(dir);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (!dir.equals(dest)){
                dir = dest.relativize(dir).normalize();
                if (exclude != null && exclude.matches(dir)){
                    return FileVisitResult.SKIP_SUBTREE;
                }
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            file = dest.relativize(file).normalize();
            if (exclude == null || !exclude.matches(file)){
                destFiles.add(file);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }
    }
}