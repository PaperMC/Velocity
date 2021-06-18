/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.network.java.serialization.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.velocitypowered.api.proxy.player.java.JavaPlayerIdentity;
import com.velocitypowered.api.proxy.player.java.JavaPlayerIdentity.Property;
import java.lang.reflect.Type;
import java.util.List;

public final class JavaPlayerIdentitySerializer implements JsonSerializer<JavaPlayerIdentity>,
    JsonDeserializer<JavaPlayerIdentity> {

  public static final JavaPlayerIdentitySerializer INSTANCE = new JavaPlayerIdentitySerializer();
  private static final Type propertyList = new TypeToken<List<Property>>() {}.getType();

  private JavaPlayerIdentitySerializer() {

  }

  @Override
  public JavaPlayerIdentity deserialize(JsonElement json, Type typeOfT,
      JsonDeserializationContext context) {
    JsonObject obj = json.getAsJsonObject();
    return new JavaPlayerIdentity(obj.get("id").getAsString(), obj.get("name").getAsString(),
        context.deserialize(obj.get("properties"), propertyList));
  }

  @Override
  public JsonElement serialize(JavaPlayerIdentity src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject obj = new JsonObject();
    obj.add("id", new JsonPrimitive(src.undashedId()));
    obj.add("name", new JsonPrimitive(src.name()));
    obj.add("properties", context.serialize(src.properties(), propertyList));
    return obj;
  }
}
