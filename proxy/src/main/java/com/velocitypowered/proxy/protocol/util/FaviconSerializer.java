package com.velocitypowered.proxy.protocol.util;

import com.google.gson.*;
import com.velocitypowered.api.server.Favicon;

import java.lang.reflect.Type;

public class FaviconSerializer implements JsonSerializer<Favicon>, JsonDeserializer<Favicon> {
    @Override
    public Favicon deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return new Favicon(json.getAsString());
    }

    @Override
    public JsonElement serialize(Favicon src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.getBase64Url());
    }
}
