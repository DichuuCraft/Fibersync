package com.hadroncfy.fibersync;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonParseException;
import com.hadroncfy.fibersync.command.BackupCommand;
import com.hadroncfy.fibersync.config.Config;
import com.hadroncfy.fibersync.config.Formats;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;

public class FibersyncMod implements ModInitializer {
    private static final Logger LOGGER = LogManager.getLogger();
    private static Config config;
    public static final BackupCommand BACKUP_COMMAND = new BackupCommand(() -> config);

    public static void loadConfig() throws IOException, JsonParseException {
        File c = new File("config", "fibersync.json");
        if (c.exists()){
            try (Reader reader = new InputStreamReader(new FileInputStream(c), StandardCharsets.UTF_8)){
                config = Config.GSON.fromJson(reader, Config.class);
            }
        }
        else {
            config = new Config();
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(c), StandardCharsets.UTF_8)){
            writer.write(Config.GSON.toJson(config));
        }
    }

    @Override
    public void onInitialize() {
        try {
            loadConfig();
        }
        catch(JsonParseException | IOException e){
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