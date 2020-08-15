package com.hadroncfy.fibersync.util.copy;

import java.nio.file.Path;

public interface IPathFilter {
    boolean accept(Path path);
}