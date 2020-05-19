package com.hadroncfy.fibersync.config;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class SimpleDateFormatSerializer
        implements JsonDeserializer<SimpleDateFormat>, JsonSerializer<SimpleDateFormat> {

    @Override
    public JsonElement serialize(SimpleDateFormat src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.toPattern());
    }

    @Override
    public SimpleDateFormat deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        try {
            return new SimpleDateFormat(json.getAsString());
        }
        catch (IllegalArgumentException e){
            throw new JsonParseException("Failed to parse date format", e);
        }
    }
    
}