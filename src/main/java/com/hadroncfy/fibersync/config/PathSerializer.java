package com.hadroncfy.fibersync.config;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class PathSerializer implements JsonDeserializer<Path>, JsonSerializer<Path> {

    @Override
    public JsonElement serialize(Path src, Type typeOfSrc, JsonSerializationContext context) {
        if (src == null){
            return JsonNull.INSTANCE;
        }
        else {
            return new JsonPrimitive(src.toString());
        }
    }

    @Override
    public Path deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        if (json.isJsonNull()){
            return null;
        }
        else {
            return Paths.get(json.getAsString());
        }
    }
    
}