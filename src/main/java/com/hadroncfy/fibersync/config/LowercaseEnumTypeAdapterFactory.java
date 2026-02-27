package com.hadroncfy.fibersync.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public final class LowercaseEnumTypeAdapterFactory implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<? super T> raw = type.getRawType();
        if (!raw.isEnum()) {
            return null;
        }

        final Map<String, T> nameToConstant = new HashMap<>();
        @SuppressWarnings("unchecked")
        T[] constants = (T[]) raw.getEnumConstants();
        for (T constant : constants) {
            nameToConstant.put(constant.toString().toLowerCase(Locale.ROOT), constant);
        }

        return new TypeAdapter<>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                if (value == null) {
                    out.nullValue();
                    return;
                }
                out.value(value.toString().toLowerCase(Locale.ROOT));
            }

            @Override
            public T read(JsonReader in) throws IOException {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }
                return nameToConstant.get(in.nextString().toLowerCase(Locale.ROOT));
            }
        };
    }
}
