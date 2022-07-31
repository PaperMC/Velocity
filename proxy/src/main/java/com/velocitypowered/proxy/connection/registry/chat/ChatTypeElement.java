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

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.registry.ChatData;
import java.util.Locale;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ChatTypeElement {


  @Nullable
  private final ElementType type;
  @Nullable
  private final ChatDecoration decoration;
  private final ChatData.@Nullable Priority priority;

  public ChatTypeElement(ElementType type, ChatDecoration decoration, ChatData.Priority priority) {
    this.type = type;
    this.decoration = decoration;
    this.priority = priority;
  }

  public static ChatTypeElement decodeFromRegistry(ElementType type, CompoundBinaryTag elementCompound, ProtocolVersion version) {
    ChatDecoration decoration = null;
    ChatData.Priority priority = null;
    final CompoundBinaryTag decorationCompound = (CompoundBinaryTag) elementCompound.get("decoration");
    if (decorationCompound != null) {
      decoration = ChatDecoration.decodeRegistryEntry(decorationCompound, version);
    }

    if (elementCompound.get("priority") != null) {
      priority = ChatData.Priority.valueOf(elementCompound.getString("priority").toUpperCase(Locale.ROOT));
    }
    return new ChatTypeElement(type, decoration, priority);
  }

  public CompoundBinaryTag encodeForRegistry(ProtocolVersion version) {
    final CompoundBinaryTag.Builder compoundBuilder = CompoundBinaryTag.builder();
    if (priority != null) {
      compoundBuilder.put("priority", StringBinaryTag.of(priority.name().toLowerCase(Locale.ROOT)));
    }

    if (decoration != null) {
      compoundBuilder.put("decoration", decoration.encodeRegistryEntry(version));
    }

    return compoundBuilder.build();
  }

  public enum ElementType {
    CHAT,
    OVERLAY,
    NARRATION
  }
}
