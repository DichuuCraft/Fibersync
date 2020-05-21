package com.hadroncfy.fibersync.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileUtil {
    private static final Logger LOGGER = LogManager.getLogger();
    private static int runrsync(String rsyncPath, File src, File dest) throws IOException, InterruptedException {
        List<String> argv = new ArrayList<>();
        argv.add(rsyncPath);
        argv.add("-r");
        // argv.add("--delete");
        for (File f: src.listFiles()){
            if (!f.getName().equals("session.lock")){
                argv.add(f.getAbsolutePath());
            }
        }
        argv.add(dest.getAbsolutePath());
        String[] args = new String[argv.size()];
        argv.toArray(args);

        return new ProcessBuilder().inheritIO().command(args).start().waitFor();
    }
    public static void rsync(String rsyncPath, File src, File dest){
        try {
            if (0 != runrsync(rsyncPath, src, dest)){
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static byte[] checkSum(Path f1) throws IOException, NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance("md5");
        try (InputStream is = Files.newInputStream(f1)){
            DigestInputStream dis = new DigestInputStream(is, md);
            byte[] buffer = new byte[1024];
            int rn;
            do {
                rn = dis.read(buffer);
            } while(rn != -1);
            
            return md.digest();
        }
    }
}