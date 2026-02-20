/*
 * Copyright (C) 2018-2023 Velocity Contributors
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

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;

/**
 * Serializer for time-based arguments represented as {@link Integer}.
 *
 * <p>This class handles the serialization and deserialization of time-related arguments,
 * converting them to and from an {@link Integer} format.</p>
 */
public class TimeArgumentSerializer implements ArgumentPropertySerializer<Integer> {

  static final TimeArgumentSerializer TIME = new TimeArgumentSerializer();

  @Override
  public Integer deserialize(ByteBuf buf, ProtocolVersion protocolVersion) {
    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_19_4)) {
      return buf.readInt();
    }
    return 0;
  }

  @Override
  public void serialize(Integer object, ByteBuf buf, ProtocolVersion protocolVersion) {
    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_19_4)) {
      buf.writeInt(object);
    }
  }
}
