package com.velocitypowered.proxy.protocol.util;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.velocitypowered.api.util.Favicon;
import java.lang.reflect.Type;

public final class FaviconSerializer implements JsonSerializer<Favicon>, JsonDeserializer<Favicon> {

  @Override
  public Favicon deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
    return new Favicon(json.getAsString());
  }

  @Override
  public JsonElement serialize(Favicon src, Type typeOfSrc, JsonSerializationContext context) {
    return new JsonPrimitive(src.getBase64Url());
  }
}
