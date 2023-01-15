/*
 * Copyright (C) 2020-2023 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.util;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.velocitypowered.api.util.Favicon;
import java.lang.reflect.Type;

/**
 * Serializes {@link Favicon} instances into JSON.
 */
public final class FaviconSerializer implements JsonSerializer<Favicon>, JsonDeserializer<Favicon> {

  public static final FaviconSerializer INSTANCE = new FaviconSerializer();

  private FaviconSerializer() {

  }

  @Override
  public Favicon deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
    return new Favicon(json.getAsString());
  }

  @Override
  public JsonElement serialize(Favicon src, Type typeOfSrc, JsonSerializationContext context) {
    return new JsonPrimitive(src.getBase64Url());
  }
}
