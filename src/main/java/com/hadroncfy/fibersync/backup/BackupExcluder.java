package com.hadroncfy.fibersync.backup;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class BackupExcluder implements PathMatcher {
    public static final int MASK_NONE      = 0;
    public static final int MASK_OTHER     = 1;
    public static final int MASK_OVERWORLD = 2;
    public static final int MASK_NETHER    = 4;
    public static final int MASK_THE_END   = 8;
    public static final int MASK_ALL       = 15;

    private static final Set<Path> OVERWORLD_PATHS = new HashSet<>();
    private static final Path NETHER_PATH = Paths.get("DIM-1");
    private static final Path THE_END_PATH = Paths.get("DIM1");
    private static final Path SESSION_LOCK = Paths.get("session.lock");

    static {
        OVERWORLD_PATHS.add(Paths.get("region"));
        OVERWORLD_PATHS.add(Paths.get("poi"));
        OVERWORLD_PATHS.add(Paths.get("data"));
    }

    private final PathMatcher parent;
    private final int mask;

    public BackupExcluder(PathMatcher parent, int mask){
        this.parent = parent;
        this.mask = mask;
    }

    @Override
    public boolean matches(Path path) {
        if (SESSION_LOCK.equals(path)) return true;
        if (parent.matches(path)) return true;
        if (path.getNameCount() > 0){
            Path p2 = path.getName(0);

            if (OVERWORLD_PATHS.contains(p2)){
                return (mask & MASK_OVERWORLD) != 0;
            }
            else if (NETHER_PATH.equals(p2)){
                return (mask & MASK_NETHER) != 0;
            }
            else if (THE_END_PATH.equals(p2)){
                return (mask & MASK_THE_END) != 0;
            }
            else {
                return (mask & MASK_OTHER) != 0;
            }
        }
        return false;
    }

    
}