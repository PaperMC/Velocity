/*
 * Copyright (C) 2018-2022 Velocity Contributors
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
 * An argument property serializer that will serialize and deserialize nothing.
 */
class EmptyArgumentPropertySerializer implements ArgumentPropertySerializer<Void> {

  static final ArgumentPropertySerializer<Void> EMPTY =
      new EmptyArgumentPropertySerializer();

  private EmptyArgumentPropertySerializer() {
  }

  @Override
  public @Nullable Void deserialize(ByteBuf buf, ProtocolVersion protocolVersion) {
    return null;
  }

  @Override
  public void serialize(Void object, ByteBuf buf, ProtocolVersion protocolVersion) {

  }
}
