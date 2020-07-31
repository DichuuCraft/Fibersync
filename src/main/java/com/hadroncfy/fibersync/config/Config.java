package com.hadroncfy.fibersync.config;

import java.io.File;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hadroncfy.fibersync.Mode;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.LowercaseEnumTypeAdapterFactory;

public class Config {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .registerTypeHierarchyAdapter(Text.class, new Text.Serializer())
        .registerTypeHierarchyAdapter(Style.class, new Style.Serializer())
        .registerTypeAdapterFactory(new LowercaseEnumTypeAdapterFactory())
        .registerTypeAdapter(SimpleDateFormat.class, new SimpleDateFormatSerializer())
        .registerTypeHierarchyAdapter(Path.class, new PathSerializer()).create();
    private static final Set<String> DEFAULT_ALT_PREFIX = new HashSet<>();
    private static final Path configPath = new File("config").toPath();

    static {
        DEFAULT_ALT_PREFIX.add("!!fs");
        DEFAULT_ALT_PREFIX.add("!!qb");
    }

    public Path backupDir = configPath.resolve("fibersync").resolve("backups");
    public Path tempDir = configPath.resolve("fibersync").resolve("temp");
    public SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
    public Set<String> alternativeCmdPrefix = DEFAULT_ALT_PREFIX;
    public int defaultCountDown = 10, maxBackupCount = 5;
    public String oldWorldName = "oldworld", oldWorldDescription = "回档前自动备份";
    public Mode mode = Mode.BACKUP;

    public Formats formats = new Formats();
}