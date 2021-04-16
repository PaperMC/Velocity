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

package com.velocitypowered.proxy.network.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import java.util.function.Supplier;

public interface PacketReader<P extends Packet> {
  P read(final ByteBuf buf, final ProtocolVersion version);

  default int expectedMinLength(ByteBuf buf, PacketDirection direction, ProtocolVersion version) {
    return 0;
  }

  default int expectedMaxLength(ByteBuf buf, PacketDirection direction, ProtocolVersion version) {
    return -1;
  }

  static <P extends Packet> PacketReader<P> unsupported() {
    return (buf, version) -> {
      throw new UnsupportedOperationException();
    };
  }

  static <P extends Packet> PacketReader<P> instance(final P packet) {
    return (buf, version) -> packet;
  }

  @Deprecated
  static <P extends Packet> PacketReader<P> method(final Supplier<P> factory) {
    return (buf, version) -> {
      final P packet = factory.get();
      packet.decode(buf, null, version);
      return packet;
    };
  }
}
