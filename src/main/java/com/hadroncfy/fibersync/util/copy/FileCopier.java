package com.hadroncfy.fibersync.util.copy;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hadroncfy.fibersync.FibersyncMod;
import com.hadroncfy.fibersync.util.FileUtil;

public class FileCopier {
    public final FileSkipMode skip_mode;
    private final Path src;
    private final Path dest;
    private final List<Path> srcFiles = new ArrayList<>();
    private final List<Path> destFiles = new ArrayList<>();
    private final Set<Path> srcFileSet = new HashSet<>();

    private FileOperationProgressListener listener;
    private PathMatcher exclude;
    private long totalSize = 0;

    public FileCopier(Path src, Path dest, FileSkipMode skip_mode) {
        this.src = src;
        this.dest = dest;
        this.skip_mode = skip_mode;
    }

    public FileCopier setListener(FileOperationProgressListener listener) {
        this.listener = listener;
        return this;
    }

    public FileCopier setExclude(PathMatcher exclude) {
        this.exclude = exclude;
        return this;
    }

    private static boolean checkSum(MessageDigest md, Path f1, Path f2) throws IOException {
        if (Files.size(f1) != Files.size(f2)) return false;
        final byte[] b1 = FileUtil.checkSum(md, f1), b2 = FileUtil.checkSum(md, f2);
        for (int i = 0; i < b1.length; i++) {
            if (b1[i] != b2[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean shoudSkipFile(Path src, Path dest, FileSkipMode mode, MessageDigest md) {
        try {
            switch (mode) {
                default:
                case NEVER: return false;
                case CHECKSUM: return checkSum(md, src, dest);
                case MOTD: return Files.size(src) == Files.size(dest) && Files.getLastModifiedTime(src).equals(Files.getLastModifiedTime(dest));
            }
        } catch (IOException e) {
            return false;
        }
    }

    public long run() throws IOException, NoSuchAlgorithmException {
        var md = MessageDigest.getInstance("md5");
        Files.walkFileTree(src, new SourceFileVisitor());
        Files.walkFileTree(dest, new DestFileVisitor());

        if (this.listener != null) {
            this.listener.start(totalSize);
        }

        for (Path src1 : srcFiles) {
            final Path dest1 =this.dest.resolve(src1);
            src1 = this.src.resolve(src1);
            if (Files.exists(dest1)) {
                if (Files.isDirectory(dest1)) {
                    if (!Files.isDirectory(src1)) {
                        new FileDeleter(dest1).run();
                    } else {
                        continue;
                    }
                } else {
                    if (Files.isDirectory(src1)) {
                        Files.delete(dest1);
                    } else if (shoudSkipFile(src1, dest1, this.skip_mode, md)) {
                        FibersyncMod.LOGGER.debug("Skipping non-modified file {}", src1);
                        if (this.listener != null){
                            this.listener.onFileDone(src1, Files.size(src1));
                        }
                        continue;
                    }
                }
            }

            final var start = System.currentTimeMillis();
            Files.copy(src1, dest1, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            Files.setLastModifiedTime(dest1, Files.getLastModifiedTime(src1));
            final var elapsed = System.currentTimeMillis() - start;
            FibersyncMod.LOGGER.debug("Copied file (or dir) {} to {}, {}M/s", src1, dest1, (double) Files.size(src1) / elapsed / 1000D);

            if (listener != null){
                listener.onFileDone(src1, Files.size(src1));
            }
        }

        for (Path dest1 : destFiles) {
            if (!srcFileSet.contains(dest1)) {
                Files.delete(dest.resolve(dest1));
                FibersyncMod.LOGGER.debug("deleted redundant file {}", dest1);
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
                } else {
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
            if (!dir.equals(dest)){
                dir = dest.relativize(dir).normalize();
                destFiles.add(dir);
            }
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