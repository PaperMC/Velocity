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

package com.velocitypowered.proxy.protocol;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import io.netty.buffer.ByteBuf;

/**
 * Represents a Minecraft packet.
 */
public interface MinecraftPacket {

  void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion);

  void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion);

  boolean handle(MinecraftSessionHandler handler);

  default int decodeExpectedMaxLength(ByteBuf buf, ProtocolUtils.Direction direction,
      ProtocolVersion version) {
    return -1;
  }

  default int decodeExpectedMinLength(ByteBuf buf, ProtocolUtils.Direction direction,
      ProtocolVersion version) {
    return 0;
  }

  default int encodeSizeHint(ProtocolUtils.Direction direction,
      ProtocolVersion version) {
    return -1;
  }

  /**
   * Indicates whether this packet overrides {@link #decodeExpectedMinLength} / {@link #decodeExpectedMaxLength}
   * to enforce wire-size bounds during decoding.
   *
   * <p>bVelocity: the decoder only calls the length-sanity check path when this is {@code true}. The
   * vast majority of packets do not override those methods (they return the inert {@code -1}/{@code 0}
   * defaults), so skipping two virtual dispatches per inbound packet on the hot decode path is a
   * worthwhile win. Packets that genuinely enforce bounds override this to return {@code true}.
   *
   * @return {@code true} if the decoder should run length sanity checks for this packet
   */
  default boolean hasLengthChecks() {
    return false;
  }
}
