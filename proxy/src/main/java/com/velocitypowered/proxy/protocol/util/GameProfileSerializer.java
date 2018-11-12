package com.velocitypowered.proxy.protocol.util;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.GameProfile.Property;
import java.lang.reflect.Type;
import java.util.List;

public class GameProfileSerializer implements JsonSerializer<GameProfile>,
    JsonDeserializer<GameProfile> {

  private static final Type propertyList = new TypeToken<List<Property>>() {}.getType();

  @Override
  public GameProfile deserialize(JsonElement json, Type typeOfT,
      JsonDeserializationContext context) {
    JsonObject obj = json.getAsJsonObject();
    return new GameProfile(obj.get("id").getAsString(), obj.get("name").getAsString(),
        context.deserialize(obj.get("properties"), propertyList));
  }

  @Override
  public JsonElement serialize(GameProfile src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject obj = new JsonObject();
    obj.add("id", new JsonPrimitive(src.getUndashedId()));
    obj.add("name", new JsonPrimitive(src.getName()));
    obj.add("properties", context.serialize(src.getProperties(), propertyList));
    return obj;
  }
}
