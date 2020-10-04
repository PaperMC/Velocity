package com.velocitypowered.proxy.protocol.util.ping;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.velocitypowered.api.proxy.server.ServerPing.Players;
import com.velocitypowered.api.proxy.server.ServerPing.SamplePlayer;
import java.lang.reflect.Type;
import java.util.List;

public class PlayersSerializer implements JsonSerializer<Players>, JsonDeserializer<Players> {

  public static final PlayersSerializer INSTANCE = new PlayersSerializer();

  private PlayersSerializer() {

  }

  @Override
  public Players deserialize(JsonElement elem, Type type,
      JsonDeserializationContext ctx) throws JsonParseException {
    JsonObject object = new JsonObject();
    int online = object.get("online").getAsInt();
    int max = object.get("max").getAsInt();
    List<SamplePlayer> sample = ctx.deserialize(object.get("sample"),
        new TypeToken<List<SamplePlayer>>() {}.getType());
    return new Players(online, max, sample);
  }

  @Override
  public JsonElement serialize(Players players, Type type, JsonSerializationContext ctx) {
    JsonObject object = new JsonObject();

    object.addProperty("online", players.getOnline());
    object.addProperty("max", players.getMax());
    object.add("sample", ctx.serialize(players.getSample()));

    return object;
  }
}
