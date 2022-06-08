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
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextFormat;
import net.kyori.adventure.translation.Translatable;
import org.jetbrains.annotations.NotNull;

// TODO Implement
public class ChatData {

  private static final ListBinaryTag EMPTY_LIST_TAG = ListBinaryTag.empty();
  private final String identifier;
  private final int id;
  private final Decoration chatDecoration;
  private Decoration narrationDecoration;

  public ChatData(int id, String identifier, Decoration chatDecoration, Decoration narrationDecoration) {
    this.id = id;
    this.identifier = identifier;
    this.chatDecoration = chatDecoration;
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
    ChatData decodedChatData = decodeElementCompound(element);
    return decodedChatData.annotateWith(id, registryIdentifier);
  }

  private ChatData annotateWith(Integer id, String registryIdentifier) {
    return new ChatData(id, registryIdentifier, this.chatDecoration, this.narrationDecoration);
  }

  private static ChatData decodeElementCompound(CompoundBinaryTag element) {
    Decoration chatDecoration = null;
    Decoration narrationDecoration = null;

    final CompoundBinaryTag chatCompound = element.getCompound("chat");
    final CompoundBinaryTag chatDecorationCompound = chatCompound.getCompound("decoration");
    if (chatDecorationCompound != CompoundBinaryTag.empty()) {
      chatDecoration = Decoration.decodeRegistryEntry(chatDecorationCompound);
    }

    final CompoundBinaryTag narrationCompound = element.getCompound("narration");
    final CompoundBinaryTag narrationDecorationCompound = narrationCompound.getCompound("decoration");
    if (narrationDecorationCompound != CompoundBinaryTag.empty()) {
      narrationDecoration = Decoration.decodeRegistryEntry(narrationCompound);
    }

    return new ChatData(-1, "invalid", chatDecoration, narrationDecoration);
  }

  public String getIdentifier() {
    return identifier;
  }

  public int getId() {
    return id;
  }




  public static class Decoration implements Translatable {

    private final List<String> parameters;
    private final List<TextFormat> style;
    private final String translationKey;

    public List<String> getParameters() {
      return parameters;
    }

    public List<TextFormat> getStyle() {
      return style;
    }

    @Override
    public @NotNull String translationKey() {
      return translationKey;
    }

    /**
     * Creates a Decoration with the associated data.
     * @param parameters chat params
     * @param style chat style
     * @param translationKey translation key
     */
    public Decoration(List<String> parameters, List<TextFormat> style, String translationKey) {
      this.parameters = Preconditions.checkNotNull(parameters);
      this.style = Preconditions.checkNotNull(style);
      this.translationKey = Preconditions.checkNotNull(translationKey);
      Preconditions.checkArgument(translationKey.length() > 0);
    }

    /**
     * Decodes a decoration entry.
     * @param toDecode Compound Tag to decode
     * @return the parsed Decoration entry.
     */
    public static Decoration decodeRegistryEntry(CompoundBinaryTag toDecode) {
      ImmutableList.Builder<String> parameters = ImmutableList.builder();
      ListBinaryTag paramList = toDecode.getList("parameters", EMPTY_LIST_TAG);
      if (paramList != EMPTY_LIST_TAG) {
        paramList.forEach(binaryTag -> parameters.add(binaryTag.toString()));
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

      return new Decoration(parameters.build(), style.build(), translationKey);
    }

    public void encodeRegistryEntry(CompoundBinaryTag compoundBinaryTag) {}

  }

  public static enum Priority {
    SYSTEM,
    CHAT
  }
}
