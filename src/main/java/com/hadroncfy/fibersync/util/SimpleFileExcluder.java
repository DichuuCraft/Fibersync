package com.hadroncfy.fibersync.util;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;

public class SimpleFileExcluder implements PathMatcher {
    private final List<Path> paths = new ArrayList<>();

    public void add(Path path){
        paths.add(path);
    }

    @Override
    public boolean matches(Path pathname) {
        for (Path path: paths){
            if (pathname.equals(path)){
                return true;
            }
        }
        return false;
    }

    public List<Path> getPaths(){
        return paths;
    }
}