package com.hadroncfy.fibersync.config;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

import com.mojang.serialization.JsonOps;

public final class TextJsonAdapter implements JsonSerializer<Text>, JsonDeserializer<Text> {
    @Override
    public JsonElement serialize(Text src, Type typeOfSrc, JsonSerializationContext context) {
        if (src == null) {
            return JsonNull.INSTANCE;
        }
        return TextCodecs.CODEC.encodeStart(JsonOps.INSTANCE, src)
            .resultOrPartial(msg -> {})
            .orElse(JsonNull.INSTANCE);
    }

    @Override
    public Text deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
        if (json == null || json.isJsonNull()) {
            return Text.empty();
        }
        return TextCodecs.CODEC.parse(JsonOps.INSTANCE, json)
            .resultOrPartial(msg -> {})
            .orElse(Text.empty());
    }
}
