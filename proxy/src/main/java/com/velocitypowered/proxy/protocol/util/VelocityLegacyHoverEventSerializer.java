package com.velocitypowered.proxy.protocol.util;

import java.io.IOException;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.TagStringIO;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEvent.ShowEntity;
import net.kyori.adventure.text.event.HoverEvent.ShowItem;
import net.kyori.adventure.text.serializer.gson.LegacyHoverEventSerializer;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import net.kyori.adventure.util.Codec.Decoder;
import net.kyori.adventure.util.Codec.Encoder;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * An implementation of {@link LegacyHoverEventSerializer} that implements the interface in the
 * most literal, albeit "incompatible" way possible.
 */
public class VelocityLegacyHoverEventSerializer implements LegacyHoverEventSerializer {

  public static final LegacyHoverEventSerializer INSTANCE =
      new VelocityLegacyHoverEventSerializer();

  private VelocityLegacyHoverEventSerializer() {

  }

  private static Key legacyIdToFakeKey(byte id) {
    return Key.of("velocity", "legacy_hover/id_" + id);
  }

  @Override
  public HoverEvent.@NonNull ShowItem deserializeShowItem(@NonNull Component input)
      throws IOException {
    String snbt = PlainComponentSerializer.plain().serialize(input);
    CompoundBinaryTag item = TagStringIO.get().asCompound(snbt);

    Key key;
    String idIfString = item.getString("id", "");
    if (idIfString.isEmpty()) {
      key = legacyIdToFakeKey(item.getByte("id"));
    } else {
      key = Key.of(idIfString);
    }

    byte count = item.getByte("Count", (byte) 1);
    return new ShowItem(key, count, BinaryTagHolder.of(snbt));
  }

  @Override
  public HoverEvent.@NonNull ShowEntity deserializeShowEntity(@NonNull Component input,
      Decoder<Component, String, ? extends RuntimeException> componentDecoder) throws IOException {
    String snbt = PlainComponentSerializer.plain().serialize(input);
    CompoundBinaryTag item = TagStringIO.get().asCompound(snbt);

    Component name;
    try {
      name = componentDecoder.decode(item.getString("name"));
    } catch (Exception e) {
      name = TextComponent.of(item.getString("name"));
    }

    return new ShowEntity(Key.of(item.getString("type")),
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
    if (input.nbt() != null) {
      builder.put("tag", TagStringIO.get().asCompound(input.nbt().string()));
    }

    return TextComponent.of(TagStringIO.get().asString(builder.build()));
  }

  @Override
  public @NonNull Component serializeShowEntity(HoverEvent.@NonNull ShowEntity input,
      Encoder<Component, String, ? extends RuntimeException> componentEncoder) throws IOException {
    CompoundBinaryTag.Builder tag = CompoundBinaryTag.builder()
        .putString("id", input.id().toString())
        .putString("type", input.type().asString());
    if (input.name() != null) {
      tag.putString("name", componentEncoder.encode(input.name()));
    }
    return TextComponent.of(TagStringIO.get().asString(tag.build()));
  }
}
