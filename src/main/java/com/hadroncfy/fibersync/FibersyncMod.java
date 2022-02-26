package com.hadroncfy.fibersync;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.gson.JsonParseException;
import com.hadroncfy.fibersync.config.Config;
import com.hadroncfy.fibersync.config.Formats;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;

public class FibersyncMod implements ModInitializer {
    private static final Logger LOGGER = LogManager.getLogger();
    private static Config config;

    public static void loadConfig() throws IOException, JsonParseException {
        var dir = Paths.get("config");
        if (!Files.exists(dir)){
            Files.createDirectories(dir);
        }
        var c = dir.resolve("fibersync.json");
        if (Files.exists(c)){
            try (Reader reader = Files.newBufferedReader(c, StandardCharsets.UTF_8)) {
                config = Config.GSON.fromJson(reader, Config.class);
            }
        } else {
            config = new Config();
        }
        try (Writer writer = Files.newBufferedWriter(c, StandardCharsets.UTF_8)) {
            writer.write(Config.GSON.toJson(config));
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
        return config.formats;
    }
}