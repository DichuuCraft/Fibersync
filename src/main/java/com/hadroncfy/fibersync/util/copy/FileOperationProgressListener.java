package com.hadroncfy.fibersync.util.copy;

import java.nio.file.Path;

public interface FileOperationProgressListener {
    void start(int fileCount);
    void onFileDone(Path file);
    void done();
}