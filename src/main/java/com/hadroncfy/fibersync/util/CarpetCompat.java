package com.hadroncfy.fibersync.util;

import java.lang.reflect.Field;

public final class CarpetCompat {
    private static volatile Field lastSpawnerField;

    private CarpetCompat() {
    }

    public static boolean isLastSpawnerNull() {
        try {
            Field f = lastSpawnerField;
            if (f == null) {
                f = Class.forName("carpet.utils.SpawnReporter").getDeclaredField("lastSpawner");
                f.setAccessible(true);
                lastSpawnerField = f;
            }
            return f.get(null) == null;
        } catch (Throwable t) {
            return false;
        }
    }
}
