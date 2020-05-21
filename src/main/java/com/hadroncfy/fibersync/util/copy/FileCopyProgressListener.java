package com.hadroncfy.fibersync.util.copy;

import java.nio.file.Path;

public interface FileCopyProgressListener {
    void start(int fileCount);
    void onFileCopied(Path file);
    void done();
}