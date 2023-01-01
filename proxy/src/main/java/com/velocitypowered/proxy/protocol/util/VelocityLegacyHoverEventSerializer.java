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

import java.io.IOException;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.TagStringIO;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEvent.ShowEntity;
import net.kyori.adventure.text.event.HoverEvent.ShowItem;
import net.kyori.adventure.text.serializer.gson.LegacyHoverEventSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.util.Codec.Decoder;
import net.kyori.adventure.util.Codec.Encoder;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * An implementation of {@link LegacyHoverEventSerializer} that implements the interface in the most
 * literal, albeit "incompatible" way possible.
 */
public class VelocityLegacyHoverEventSerializer implements LegacyHoverEventSerializer {

  public static final LegacyHoverEventSerializer INSTANCE =
      new VelocityLegacyHoverEventSerializer();

  private VelocityLegacyHoverEventSerializer() {

  }

  private static Key legacyIdToFakeKey(byte id) {
    return Key.key("velocity", "legacy_hover/id_" + id);
  }

  @Override
  public HoverEvent.@NonNull ShowItem deserializeShowItem(@NonNull Component input)
      throws IOException {
    String snbt = PlainTextComponentSerializer.plainText().serialize(input);
    CompoundBinaryTag item = TagStringIO.get().asCompound(snbt);

    Key key;
    String idIfString = item.getString("id", "");
    if (idIfString.isEmpty()) {
      key = legacyIdToFakeKey(item.getByte("id"));
    } else {
      key = Key.key(idIfString);
    }

    byte count = item.getByte("Count", (byte) 1);
    return ShowItem.of(key, count, BinaryTagHolder.binaryTagHolder(snbt));
  }

  @Override
  public HoverEvent.@NonNull ShowEntity deserializeShowEntity(@NonNull Component input,
      Decoder<Component, String, ? extends RuntimeException> componentDecoder) throws IOException {
    String snbt = PlainTextComponentSerializer.plainText().serialize(input);
    CompoundBinaryTag item = TagStringIO.get().asCompound(snbt);

    Component name;
    try {
      name = componentDecoder.decode(item.getString("name"));
    } catch (Exception e) {
      name = Component.text(item.getString("name"));
    }

    return ShowEntity.of(Key.key(item.getString("type")),
        UUID.fromString(item.getString("id")),
        name);
  }

  @Override
  public @NonNull Component serializeShowItem(HoverEvent.@NonNull ShowItem input)
      throws IOException {
    final CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder()
        .putByte("Count", (byte) input.count());

    String keyAsString = input.item().asString();
    if (keyAsString.startsWith("velocity:legacy_hover/id_")) {
      builder.putByte("id", Byte.parseByte(keyAsString
          .substring("velocity:legacy_hover/id_".length())));
    } else {
      builder.putString("id", keyAsString);
    }

    BinaryTagHolder nbt = input.nbt();
    if (nbt != null) {
      builder.put("tag", TagStringIO.get().asCompound(nbt.string()));
    }

    return Component.text(TagStringIO.get().asString(builder.build()));
  }

  @Override
  public @NonNull Component serializeShowEntity(HoverEvent.@NonNull ShowEntity input,
      Encoder<Component, String, ? extends RuntimeException> componentEncoder) throws IOException {
    CompoundBinaryTag.Builder tag = CompoundBinaryTag.builder()
        .putString("id", input.id().toString())
        .putString("type", input.type().asString());
    Component name = input.name();
    if (name != null) {
      tag.putString("name", componentEncoder.encode(name));
    }
    return Component.text(TagStringIO.get().asString(tag.build()));
  }
}
