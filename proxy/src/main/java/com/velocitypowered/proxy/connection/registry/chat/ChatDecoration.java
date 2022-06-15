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

package com.velocitypowered.proxy.connection.registry.chat;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.network.ProtocolVersion;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextFormat;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ChatDecoration {

  private final List<String> parameters;
  private final List<TextFormat> style;
  @Nullable
  private final String translationKey;

  public List<String> getParameters() {
    return parameters;
  }

  public List<TextFormat> getStyle() {
    return style;
  }

  public @Nullable String translationKey() {
    return translationKey;
  }

  /**
   * Creates a Decoration with the associated data.
   *
   * @param parameters     chat params
   * @param style          chat style
   * @param translationKey translation key
   */
  public ChatDecoration(List<String> parameters, List<TextFormat> style, @Nullable String translationKey) {
    this.parameters = Preconditions.checkNotNull(parameters);
    this.style = Preconditions.checkNotNull(style);
    this.translationKey = translationKey;
  }

  /**
   * Decodes a decoration entry.
   *
   * @param toDecode Compound Tag to decode
   * @param version
   * @return the parsed Decoration entry.
   */
  public static ChatDecoration decodeRegistryEntry(CompoundBinaryTag toDecode, ProtocolVersion version) {
    ImmutableList.Builder<String> parameters = ImmutableList.builder();
    ListBinaryTag paramList = toDecode.getList("parameters", ListBinaryTag.empty());
    if (paramList != ListBinaryTag.empty()) {
      paramList.forEach(binaryTag -> parameters.add(((StringBinaryTag) binaryTag).value()));
    }

    ImmutableList.Builder<TextFormat> style = ImmutableList.builder();
    CompoundBinaryTag styleList = toDecode.getCompound("style");
    for (String key : styleList.keySet()) {
      if ("color".equals(key)) {
        final NamedTextColor value = NamedTextColor.NAMES.value(styleList.getString(key));
        if (value != null) {
          style.add(value);
        } else {
          throw new IllegalArgumentException("Unable to map color value: " + styleList.getString(key));
        }
      } else {
        // Key is a Style option instead
        TextDecoration deco = TextDecoration.NAMES.value(key);
        if (deco == null) {
          throw new IllegalArgumentException("Unable to map text style of: " + key);
        }
        // This wouldn't be here if it wasn't applied, but here it goes anyway:
        byte val = styleList.getByte(key);
        if (val != 0) {
          style.add(deco);
        }
      }
    }

    String translationKey = toDecode.getString("translation_key");

    return new ChatDecoration(parameters.build(), style.build(), translationKey.isEmpty() ? null : translationKey);
  }

  public CompoundBinaryTag encodeRegistryEntry(ProtocolVersion version) {

    CompoundBinaryTag.Builder compoundBinaryTag = CompoundBinaryTag.builder();

    if (translationKey != null) {
      compoundBinaryTag.put("translation_key", StringBinaryTag.of(translationKey));
    }

    final CompoundBinaryTag.Builder styleBuilder = CompoundBinaryTag.builder();
    style.forEach(styleEntry -> {
      if (styleEntry instanceof TextColor color) {
        styleBuilder.putString("color", color.toString());
      } else if (styleEntry instanceof TextDecoration decoration) {
        styleBuilder.putByte(decoration.name().toLowerCase(Locale.ROOT), (byte) 1); // This won't be here if not applied
      }
    });
    compoundBinaryTag.put("style", styleBuilder.build());

    if (parameters.size() == 0) {
      compoundBinaryTag.put("parameters", ListBinaryTag.empty());
    } else {
      final ListBinaryTag.Builder<StringBinaryTag> parametersBuilder = ListBinaryTag.builder(BinaryTagTypes.STRING);
      parameters.forEach(param -> parametersBuilder.add(StringBinaryTag.of(param)));
      compoundBinaryTag.put("parameters", parametersBuilder.build());
    }


    return compoundBinaryTag.build();
  }

}
