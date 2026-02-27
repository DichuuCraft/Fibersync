package com.hadroncfy.fibersync;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.hadroncfy.fibersync.config.Config;
import com.hadroncfy.fibersync.config.Formats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;

public class FibersyncMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("FibersyncMod");
    private static Config config;
    private static Formats formats;

    public static <T> T loadJsonConfig(Path path, Gson gson, Class<T> cls, Supplier<T> defaultCreator) {
        T config = null;
        try {
            if (Files.exists(path)){
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    config = gson.fromJson(reader, cls);
                }
            } else {
                config = defaultCreator.get();
            }
        } catch (IOException | JsonIOException e) {
            LOGGER.error("failed to load config file {}, using default", path.toString(), e);
            config = defaultCreator.get();
        }
        try {
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                writer.write(gson.toJson(config));
            }
        } catch (IOException e) {
            LOGGER.error("failed to write config file {}, ignoring", path.toString(), e);
        }
        return config;
    }

    public static void loadConfig() throws IOException, JsonParseException {
        var dir = Paths.get("config");
        if (!Files.exists(dir)){
            Files.createDirectories(dir);
        }
        config = loadJsonConfig(dir.resolve("fibersync.json"), Config.GSON, Config.class, Config::new);
        formats = loadJsonConfig(dir.resolve("formats.json"), Formats.GSON, Formats.class, Formats::new);
        formats = sanitizeFormats(formats);
        try (Writer writer = Files.newBufferedWriter(dir.resolve("formats.json"), StandardCharsets.UTF_8)) {
            writer.write(Formats.GSON.toJson(formats));
        }
    }

    @Override
    public void onInitialize() {
        try {
            loadConfig();
        } catch(Throwable e) {
            LOGGER.error("Failed to load config");
            e.printStackTrace();
            config = new Config();
        }
    }

    public static Config getConfig(){
        return config;
    }

    public static Formats getFormat(){
        return formats;
    }

    public static void saveConfig() throws IOException {
        var dir = Paths.get("config");
        if (!Files.exists(dir)){
            Files.createDirectories(dir);
        }
        try (Writer writer = Files.newBufferedWriter(dir.resolve("fibersync.json"), StandardCharsets.UTF_8)) {
            writer.write(Config.GSON.toJson(config));
        }
    }

    private static Formats sanitizeFormats(Formats loaded) {
        if (loaded == null) {
            return new Formats();
        }
        Formats defaults = new Formats();
        for (Field f : Formats.class.getFields()) {
            if (f.getType() != net.minecraft.text.Text.class) {
                continue;
            }
            try {
                net.minecraft.text.Text value = (net.minecraft.text.Text) f.get(loaded);
                if (value == null || value.getString().isEmpty()) {
                    f.set(loaded, f.get(defaults));
                }
            } catch (IllegalAccessException e) {
                // ignore
            }
        }
        return loaded;
    }
}
