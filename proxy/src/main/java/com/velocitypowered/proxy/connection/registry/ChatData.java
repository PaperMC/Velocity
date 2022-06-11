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

package com.velocitypowered.proxy.connection.registry;

/*
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import net.kyori.adventure.nbt.*;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextFormat;
import net.kyori.adventure.translation.Translatable;
import org.jetbrains.annotations.NotNull;

import java.util.List;
*/

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.network.ProtocolVersion;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextFormat;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

// TODO Implement
public class ChatData {

  private static final ListBinaryTag EMPTY_LIST_TAG = ListBinaryTag.empty();
  private final String identifier;
  private final int id;
  @Nullable
  private final Decoration chatDecoration;
  @Nullable
  private final Priority narrationPriority;
  // TODO: move to own thing?
  @Nullable
  private final Decoration narrationDecoration;

  /**
   * Represents a ChatRegistry entry.
   *
   * @param id                  chat type id
   * @param identifier          chat type identifier
   * @param chatDecoration      chat decoration
   * @param narrationDecoration narration decoration
   */
  public ChatData(int id, String identifier, @Nullable Decoration chatDecoration, @Nullable Priority narrationPriority, @Nullable Decoration narrationDecoration) {
    this.id = id;
    this.identifier = identifier;
    this.chatDecoration = chatDecoration;
    this.narrationPriority = narrationPriority;
    this.narrationDecoration = narrationDecoration;
  }

  /**
   * Decodes an entry in the registry.
   *
   * @param binaryTag the binary tag to decode.
   * @param version   the version to decode for.
   * @return The decoded ChatData
   */
  public static ChatData decodeRegistryEntry(CompoundBinaryTag binaryTag, ProtocolVersion version) {
    final String registryIdentifier = binaryTag.getString("name");
    final Integer id = binaryTag.getInt("id");

    CompoundBinaryTag element = binaryTag.getCompound("element");
    ChatData decodedChatData = decodeElementCompound(element, version);
    return decodedChatData.annotateWith(id, registryIdentifier);
  }

  private ChatData annotateWith(Integer id, String registryIdentifier) {
    return new ChatData(id, registryIdentifier, this.chatDecoration, this.narrationPriority, this.narrationDecoration);
  }

  private static ChatData decodeElementCompound(CompoundBinaryTag element, ProtocolVersion version) {
    Decoration chatDecoration = null;
    Decoration narrationDecoration = null;
    Priority narrationPriority = null;

    final CompoundBinaryTag chatCompound = element.getCompound("chat");
    final CompoundBinaryTag chatDecorationCompound = (CompoundBinaryTag) chatCompound.get("decoration");
    if (chatDecorationCompound != null) {
      chatDecoration = Decoration.decodeRegistryEntry(chatDecorationCompound);
    }

    final CompoundBinaryTag narrationCompound = element.getCompound("narration");
    final String priorityString = narrationCompound.getString("priority");
    if (!priorityString.isEmpty()) {
      narrationPriority = Priority.valueOf(priorityString.toUpperCase(Locale.ROOT));
    }

    final CompoundBinaryTag narrationDecorationCompound = (CompoundBinaryTag) narrationCompound.get("decoration");
    if (narrationDecorationCompound != null) {
      narrationDecoration = Decoration.decodeRegistryEntry(narrationDecorationCompound);
    }

    return new ChatData(-1, "invalid", chatDecoration, narrationPriority, narrationDecoration);
  }

  public String getIdentifier() {
    return identifier;
  }

  public int getId() {
    return id;
  }

  /**
   * Encodes the chat data for the network.
   *
   * @param version The protocol version to encode this chat data for
   * @return The encoded data structure
   */
  public CompoundBinaryTag encodeAsCompoundTag(ProtocolVersion version) {
    final CompoundBinaryTag.Builder compound = CompoundBinaryTag.builder();
    compound.putString("name", identifier);
    compound.putInt("id", id);

    final CompoundBinaryTag.Builder elementCompound = CompoundBinaryTag.builder();

    CompoundBinaryTag.@NotNull Builder chatCompound = CompoundBinaryTag.builder();
    if (chatDecoration != null) {
      chatCompound.put("decoration", chatDecoration.encodeRegistryEntry(version));
      elementCompound.put("chat", chatCompound.build());
    }

    final CompoundBinaryTag.Builder narrationCompoundBuilder = CompoundBinaryTag.builder();
    if (narrationPriority != null) {
      narrationCompoundBuilder.putString("priority", narrationPriority.name().toLowerCase(Locale.ROOT));
    }
    if (narrationDecoration != null) {
      narrationCompoundBuilder.put("decoration", narrationDecoration.encodeRegistryEntry(version));
    }
    final CompoundBinaryTag narrationCompound = narrationCompoundBuilder.build();
    if (!narrationCompound.equals(CompoundBinaryTag.empty())) {
      elementCompound.put("narration", narrationCompound);
    }

    compound.put("element", elementCompound.build());

    return compound.build();
  }


  public static class Decoration {

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
    public Decoration(List<String> parameters, List<TextFormat> style, @Nullable String translationKey) {
      this.parameters = Preconditions.checkNotNull(parameters);
      this.style = Preconditions.checkNotNull(style);
      this.translationKey = translationKey;
    }

    /**
     * Decodes a decoration entry.
     *
     * @param toDecode Compound Tag to decode
     * @return the parsed Decoration entry.
     */
    public static Decoration decodeRegistryEntry(CompoundBinaryTag toDecode) {
      ImmutableList.Builder<String> parameters = ImmutableList.builder();
      ListBinaryTag paramList = toDecode.getList("parameters", EMPTY_LIST_TAG);
      if (paramList != EMPTY_LIST_TAG) {
        paramList.forEach(binaryTag -> parameters.add(((StringBinaryTag) binaryTag).value()));
      }

      ImmutableList.Builder<TextFormat> style = ImmutableList.builder();
      CompoundBinaryTag styleList = toDecode.getCompound("style");
      for (String key : styleList.keySet()) {
        if ("color".equals(key)) {
          NamedTextColor color = Preconditions.checkNotNull(
              NamedTextColor.NAMES.value(styleList.getString(key)));
          style.add(color);
        } else {
          // Key is a Style option instead
          TextDecoration deco = TextDecoration.NAMES.value(key);
          // This wouldn't be here if it wasn't applied, but here it goes anyway:
          byte val = styleList.getByte(key);
          if (val != 0) {
            style.add(deco);
          }
        }
      }

      String translationKey = toDecode.getString("translation_key");

      return new Decoration(parameters.build(), style.build(), translationKey.isEmpty() ? null : translationKey);
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
        compoundBinaryTag.put("parameters", EMPTY_LIST_TAG);
      } else {
        final ListBinaryTag.Builder<StringBinaryTag> parametersBuilder = ListBinaryTag.builder(BinaryTagTypes.STRING);
        parameters.forEach(param -> parametersBuilder.add(StringBinaryTag.of(param)));
        compoundBinaryTag.put("parameters", parametersBuilder.build());
      }



      return compoundBinaryTag.build();
    }

  }

  public static enum Priority {
    SYSTEM,
    CHAT
  }
}
