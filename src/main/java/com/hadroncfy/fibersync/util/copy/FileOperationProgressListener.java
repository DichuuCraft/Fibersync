package com.hadroncfy.fibersync.util.copy;

import java.nio.file.Path;

public interface FileOperationProgressListener {
    void start(long totalSize);
    void onFileDone(Path file, long size);
    void done();
}