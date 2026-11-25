/*
 * Copyright (C) 2026 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet.brigadier;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;

/**
 * Serializes one of the 26.4+ string argument types ({@code minecraft:word},
 * {@code minecraft:quoted_string}, {@code minecraft:greedy_string}), which have no properties.
 */
class FixedStringArgumentSerializer implements ArgumentPropertySerializer<StringArgumentType> {

  private final StringArgumentType type;

  FixedStringArgumentSerializer(StringArgumentType type) {
    this.type = type;
  }

  @Override
  public StringArgumentType deserialize(ByteBuf buf, ProtocolVersion protocolVersion) {
    return type;
  }

  @Override
  public void serialize(StringArgumentType object, ByteBuf buf, ProtocolVersion protocolVersion) {
  }
}
