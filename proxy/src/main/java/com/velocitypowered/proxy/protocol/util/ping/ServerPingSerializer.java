package com.velocitypowered.proxy.protocol.util.ping;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.proxy.server.ServerPing.Players;
import com.velocitypowered.api.proxy.server.ServerPing.Version;
import com.velocitypowered.api.util.Favicon;
import com.velocitypowered.api.util.ModInfo;
import java.lang.reflect.Type;
import net.kyori.adventure.text.Component;

public class ServerPingSerializer implements JsonSerializer<ServerPing>,
    JsonDeserializer<ServerPing> {

  public static final ServerPingSerializer INSTANCE = new ServerPingSerializer();

  private ServerPingSerializer() {

  }

  @Override
  public ServerPing deserialize(JsonElement elem, Type type,
      JsonDeserializationContext ctx) throws JsonParseException {
    JsonObject object = elem.getAsJsonObject();

    Component description = ctx.deserialize(object.get("description"), Component.class);
    Version version = ctx.deserialize(object.get("version"), Version.class);
    Players players = ctx.deserialize(object.get("players"), Players.class);
    Favicon favicon = ctx.deserialize(object.get("favicon"), Favicon.class);
    ModInfo modInfo = ctx.deserialize(object.get("modInfo"), ModInfo.class);

    return new ServerPing(version, players, description, favicon, modInfo);
  }

  @Override
  public JsonElement serialize(ServerPing ping, Type type,
      JsonSerializationContext ctx) {
    JsonObject object = new JsonObject();
    object.add("description", ctx.serialize(ping.getDescriptionComponent()));
    object.add("version", ctx.serialize(ping.getVersion()));
    object.add("players", ctx.serialize(ping.getPlayers().orElse(null)));
    object.addProperty("favicon", ping.getFavicon().map(Favicon::getBase64Url).orElse(null));
    object.add("modInfo", ctx.serialize(ping.getModinfo().orElse(null)));
    return object;
  }
}
