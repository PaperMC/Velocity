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

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Wraps the serializer of properties an argument type only gained in a later version. Older
 * versions read and write nothing, as before.
 *
 * @param <T> the property type
 */
class SinceVersionArgumentPropertySerializer<T> implements ArgumentPropertySerializer<@Nullable T> {

  private final ProtocolVersion since;
  private final ArgumentPropertySerializer<T> serializer;

  SinceVersionArgumentPropertySerializer(ProtocolVersion since, ArgumentPropertySerializer<T> serializer) {
    this.since = since;
    this.serializer = serializer;
  }

  @Override
  public @Nullable T deserialize(ByteBuf buf, ProtocolVersion protocolVersion) {
    if (protocolVersion.lessThan(since)) {
      return null;
    }
    return serializer.deserialize(buf, protocolVersion);
  }

  @Override
  public void serialize(@Nullable T object, ByteBuf buf, ProtocolVersion protocolVersion) {
    if (object != null && protocolVersion.noLessThan(since)) {
      serializer.serialize(object, buf, protocolVersion);
    }
  }
}
