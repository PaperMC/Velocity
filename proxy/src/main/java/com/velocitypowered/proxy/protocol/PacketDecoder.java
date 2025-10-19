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
 * Decodes a packet from a ByteBuf.
 *
 * @param <T> the packet type
 */
public interface PacketDecoder<T extends MinecraftPacket> {

  /**
   * Decodes a packet from the provided ByteBuf.
   *
   * @param buf the buffer to read from
   * @param direction the direction of the packet
   * @param protocolVersion the protocol version
   * @return the decoded packet instance
   */
  T decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion);

  /**
   * Returns the expected maximum length of the packet. This is used for validation during
   * decoding. Return -1 if no maximum is enforced.
   *
   * @param buf the buffer containing the packet
   * @param direction the direction of the packet
   * @param version the protocol version
   * @return the maximum expected length, or -1 if no limit
   */
  default int decodeExpectedMaxLength(ByteBuf buf, ProtocolUtils.Direction direction,
      ProtocolVersion version) {
    return -1;
  }

  /**
   * Returns the expected minimum length of the packet. This is used for validation during
   * decoding. Return 0 if no minimum is enforced.
   *
   * @param buf the buffer containing the packet
   * @param direction the direction of the packet
   * @param version the protocol version
   * @return the minimum expected length
   */
  default int decodeExpectedMinLength(ByteBuf buf, ProtocolUtils.Direction direction,
      ProtocolVersion version) {
    return 0;
  }
}
