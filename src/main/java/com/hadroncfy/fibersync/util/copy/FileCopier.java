package com.hadroncfy.fibersync.util.copy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
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

    private static boolean checkSum(Path f1, Path f2) throws IOException, NoSuchAlgorithmException {
        final byte[] b1 = FileUtil.checkSum(f1), b2 = FileUtil.checkSum(f2);
        for (int i = 0; i < b1.length; i++){
            if (b1[i] != b2[i]){
                return false;
            }
        }
        return true;
    }

    public static void deleteFileTree(Path dir, FileOperationProgressListener listener) throws IOException {
        final List<Path> files = new ArrayList<>();
        SimpleFileVisitor visitor = new SimpleFileVisitor(f -> files.add(f), true);

        Files.walkFileTree(dir, visitor);

        if (listener != null){
            listener.start(visitor.size);
        }
        for (Path p: files){
            final File pf = p.toFile();
            if (pf.isFile() && listener != null){
                listener.onFileDone(p, pf.length());
            }
            Files.delete(p);
        }
        if (listener != null){
            listener.done();
        }
    }

    public static void copy(Path src, Path dest, FileOperationProgressListener listener) throws IOException,
            NoSuchAlgorithmException {
        final List<Path> srcFiles = new ArrayList<>();
        final List<Path> destFiles = new ArrayList<>();
        final Set<Path> srcFileSet = new HashSet<>();

        SimpleFileVisitor v = new SimpleFileVisitor(f -> {
            if (!f.equals(src)){
                f = src.relativize(f);
                srcFiles.add(f);
                srcFileSet.add(f);
            }
        }, false);
        Files.walkFileTree(src, v);
        Files.walkFileTree(dest, new SimpleFileVisitor(f -> {
            if (!f.equals(dest)){
                destFiles.add(dest.relativize(f));
            }
        }, true));
        
        final long totalSize = v.size;

        if (listener != null){
            listener.start(totalSize);
        }


        for (Path src1: srcFiles){
            final Path dest1 = dest.resolve(src1);
            src1 = src.resolve(src1);
            final File src1f = src1.toFile(), dest1f = dest1.toFile();
            if (dest1f.exists()){
                if (dest1f.isDirectory()){
                    if (src1f.isFile()){
                        deleteFileTree(dest1, null);
                    }
                    else {
                        // listener.onFileDone(src1);
                        continue;
                    }
                }
                else {
                    if (src1f.isDirectory()){
                        Files.delete(dest1);
                    }
                    else if (checkSum(src1, dest1)){
                        LOGGER.debug("Skipping non-modified file {}", src1.toString());
                        listener.onFileDone(src1, src1f.length());
                        continue;
                    }
                }
            }

            Files.copy(src1, dest1, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.debug("Copied file(or dir) {} to {}", src1.toString(), dest1.toString());

            listener.onFileDone(src1, src1f.length());
        }

        for (Path dest1: destFiles){
            if (!srcFileSet.contains(dest1)){
                dest.resolve(dest1).toFile().delete();
                LOGGER.debug("deleted redundant file {}", dest1.toString());
            }
        }
        listener.done();
    }
}