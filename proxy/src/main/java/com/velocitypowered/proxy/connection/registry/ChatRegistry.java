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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.velocitypowered.api.network.ProtocolVersion;

import java.io.IOException;
import java.util.Map;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.TagStringIO;


public final class ChatRegistry {

  private final Map<String, ChatData> registeredChatTypes;

  public ChatRegistry(ImmutableList<ChatData> chatDataImmutableList) {
    registeredChatTypes = Maps.uniqueIndex(chatDataImmutableList, ChatData::getIdentifier);
  }

  /**
   * Decodes a CompoundTag storing a Chat Type Registry.
   *
   * @param compound The Compound to decode
   * @param version  Protocol version
   * @return an ImmutableList of read ChatData
   */
  public static ImmutableList<ChatData> fromGameData(ListBinaryTag compound, ProtocolVersion version) {
    final ImmutableList.Builder<ChatData> builder = ImmutableList.builder();
    for (BinaryTag binaryTag : compound) {
      if (binaryTag instanceof CompoundBinaryTag) {
        final ChatData chatData = ChatData.decodeRegistryEntry((CompoundBinaryTag) binaryTag, version);
        builder.add(chatData);
      }
    }
    return builder.build();
  }

  /**
   * Serialises the Chat Registry for sending over the network.
   * @param version the version to encode for
   * @return The serialised tag list
   */
  public ListBinaryTag encodeRegistry(ProtocolVersion version) {
    final ListBinaryTag.Builder<BinaryTag> builder = ListBinaryTag.builder();
    for (ChatData data : registeredChatTypes.values()) {
      builder.add(data.encodeAsCompoundTag(version));
    }
    return builder.build();
  }
}
