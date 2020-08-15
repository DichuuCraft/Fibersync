package com.hadroncfy.fibersync.config;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.hadroncfy.fibersync.util.SimpleFileExcluder;

public class FileExcluderSerializer implements JsonSerializer<PathMatcher>, JsonDeserializer<PathMatcher> {

    @Override
    public PathMatcher deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        SimpleFileExcluder ret = new SimpleFileExcluder();
        for (JsonElement elem: json.getAsJsonArray()){
            ret.add(Paths.get(elem.getAsString()));
        }
        return ret;
    }

    @Override
    public JsonElement serialize(PathMatcher src, Type typeOfSrc, JsonSerializationContext context) {
        if (src instanceof SimpleFileExcluder){
            JsonArray ret = new JsonArray();
            for (Path p: ((SimpleFileExcluder)src).getPaths()){
                ret.add(new JsonPrimitive(p.toString()));
            }
            return ret;
        }
        else {
            throw new IllegalArgumentException("Don't know how to serialize " + src.toString());
        }
    }
    
}