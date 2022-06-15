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

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.registry.chat.ChatTypeElement;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.checkerframework.checker.nullness.qual.Nullable;

// TODO Implement
public class ChatData {

  private final String identifier;
  private final int id;
  private final ChatTypeElement chatElement;
  private final ChatTypeElement overlayElement;
  private final ChatTypeElement narrationElement;

  /**
   * Represents a ChatRegistry entry.
   *
   * @param id               chat type id
   * @param identifier       chat type identifier
   * @param chatElement      chat element
   * @param overlayElement   overlay element
   * @param narrationElement narration element
   */
  public ChatData(int id, String identifier, @Nullable ChatTypeElement chatElement, ChatTypeElement overlayElement,
                  @Nullable ChatTypeElement narrationElement) {
    this.id = id;
    this.identifier = identifier;
    this.chatElement = chatElement;
    this.overlayElement = overlayElement;
    this.narrationElement = narrationElement;
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
    return new ChatData(id, registryIdentifier, this.chatElement, this.overlayElement,
        this.narrationElement);
  }

  private static ChatData decodeElementCompound(CompoundBinaryTag element, ProtocolVersion version) {
    ChatTypeElement chatElement = null;
    ChatTypeElement overlayElement = null;
    ChatTypeElement narrationElement = null;

    final BinaryTag chatCompound = element.get("chat");
    if (chatCompound != null) {
      chatElement =
          ChatTypeElement.decodeFromRegistry(ChatTypeElement.ElementType.CHAT, (CompoundBinaryTag) chatCompound,
              version);
    }

    final BinaryTag overlayCompound = element.get("overlay");
    if (overlayCompound != null) {
      overlayElement =
          ChatTypeElement.decodeFromRegistry(ChatTypeElement.ElementType.OVERLAY, (CompoundBinaryTag) overlayCompound,
              version);
    }

    final BinaryTag narrationCompound = element.get("narration");
    if (narrationCompound != null) {
      narrationElement = ChatTypeElement.decodeFromRegistry(ChatTypeElement.ElementType.NARRATION,
          (CompoundBinaryTag) narrationCompound, version);
    }

    return new ChatData(-1, "invalid", chatElement, overlayElement, narrationElement);
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

    if (chatElement != null) {
      elementCompound.put("chat", chatElement.encodeForRegistry(version));
    }

    if (overlayElement != null) {
      elementCompound.put("overlay", overlayElement.encodeForRegistry(version));
    }

    if (narrationElement != null) {
      elementCompound.put("narration", narrationElement.encodeForRegistry(version));
    }

    compound.put("element", elementCompound.build());

    return compound.build();
  }


  public static enum Priority {
    SYSTEM,
    CHAT
  }
}
