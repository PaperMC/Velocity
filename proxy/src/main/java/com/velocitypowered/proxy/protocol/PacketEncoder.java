/*
 * Copyright (C) 2018-2025 Velocity Contributors
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

package com.velocitypowered.proxy.protocol;

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;

/**
 * Encodes a packet into a ByteBuf.
 *
 * @param <T> the packet type
 */
public interface PacketEncoder<T extends MinecraftPacket> {

  /**
   * Encodes the given packet into the provided ByteBuf.
   *
   * @param packet the packet to encode
   * @param buf the buffer to write to
   * @param direction the direction of the packet
   * @param protocolVersion the protocol version
   */
  void encode(T packet, ByteBuf buf, ProtocolUtils.Direction direction,
      ProtocolVersion protocolVersion);

  /**
   * Returns a hint for the expected size of the encoded packet. This is used to pre-allocate
   * buffers for better performance. Return -1 if no hint is available.
   *
   * @param packet the packet to encode
   * @param direction the direction of the packet
   * @param version the protocol version
   * @return the size hint, or -1 if unknown
   */
  default int encodeSizeHint(T packet, ProtocolUtils.Direction direction,
      ProtocolVersion version) {
    return -1;
  }
}
