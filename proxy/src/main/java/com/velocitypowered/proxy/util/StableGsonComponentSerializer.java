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

package com.velocitypowered.proxy.util;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

public class StableGsonComponentSerializer implements GsonComponentSerializer {

  private final GsonComponentSerializer serializer = GsonComponentSerializer.gson();

  @Override
  public @NotNull String serialize(@NotNull Component component) {
    return serializeAsStable(serializer.serializeToTree(component));
  }

  @Override
  public @NotNull Gson serializer() {
    return serializer.serializer();
  }

  @Override
  public @NotNull UnaryOperator<GsonBuilder> populator() {
    return serializer.populator();
  }

  @Override
  public @NotNull Component deserializeFromTree(@NotNull JsonElement input) {
    return serializer.deserializeFromTree(input);
  }

  @Override
  public @NotNull JsonElement serializeToTree(@NotNull Component component) {
    return serializer.serializeToTree(component);
  }

  @Override
  public @NotNull Component deserialize(@NotNull String input) {
    return serializer.deserialize(input);
  }

  private String serializeAsStable(JsonElement index) {
    StringWriter textData = new StringWriter();

    try {
      writeElement(index, new JsonWriter(textData));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    return textData.toString();
  }

  private boolean nullOrData(@Nullable JsonElement toCheck, JsonWriter to) throws IOException {
    if (toCheck == null || toCheck.isJsonNull()) {
      to.nullValue();
      return false;
    }
    return true;
  }

  private void writePrimitive(@Nullable JsonPrimitive toWrite, JsonWriter to) throws IOException {
    if (nullOrData(toWrite, to)) {
      if (toWrite.isNumber()) {
        to.value(toWrite.getAsNumber());
      } else if (toWrite.isBoolean()) {
        to.value(toWrite.getAsBoolean());
      } else {
        to.value(toWrite.getAsString());
      }
    }
  }

  private void writeArray(@Nullable JsonArray toWrite, JsonWriter to) throws IOException {
    if (nullOrData(toWrite, to)) {
      to.beginArray();
      for (JsonElement element : toWrite) {
        writeElement(element, to);
      }
      to.endArray();
    }
  }

  private void writeObject(@Nullable JsonObject toWrite, JsonWriter to) throws IOException {
    if (nullOrData(toWrite, to)) {
      to.beginObject();

      List<Map.Entry<String, JsonElement>> entries = Lists.newArrayList(toWrite.entrySet());
      entries.sort(Map.Entry.comparingByKey(Comparator.naturalOrder()));

      for (Map.Entry<String, JsonElement> entry : entries) {
        to.name(entry.getKey());
        writeElement(entry.getValue(), to);
      }

      to.endObject();
    }
  }

  private void writeElement(@Nullable JsonElement toWrite, JsonWriter to) throws IOException {
    if (nullOrData(toWrite, to)) {
      if (toWrite.isJsonPrimitive()) {
        writePrimitive(toWrite.getAsJsonPrimitive(), to);
      } else if (toWrite.isJsonArray()) {
        writeArray(toWrite.getAsJsonArray(), to);
      } else if (toWrite.isJsonObject()) {
        writeObject(toWrite.getAsJsonObject(), to);
      } else {
        throw new IOException("Cannot serialize " + toWrite);
      }
    }
  }

  @Override
  public @NotNull Builder toBuilder() {
    return serializer.toBuilder();
  }
}
