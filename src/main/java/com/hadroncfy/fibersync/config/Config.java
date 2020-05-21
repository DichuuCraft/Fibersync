package com.hadroncfy.fibersync.config;

import java.text.SimpleDateFormat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.LowercaseEnumTypeAdapterFactory;

public class Config {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting()
    .registerTypeHierarchyAdapter(Text.class, new Text.Serializer())
    .registerTypeHierarchyAdapter(Style.class, new Style.Serializer())
    .registerTypeAdapterFactory(new LowercaseEnumTypeAdapterFactory())
    .registerTypeAdapter(SimpleDateFormat.class, new SimpleDateFormatSerializer()).create();

    public String backupDir = "config/fibersync";
    public SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
    public int defaultCountDown = 20;

    public Formats formats = new Formats();
}