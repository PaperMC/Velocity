/*
 * Copyright (C) 2023 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

/**
 * Represents a packet that acknowledges a successful login, implementing {@link MinecraftPacket}.
 *
 * <p>The {@code LoginAcknowledgedPacket} is sent by the server to confirm that the player's login
 * process has been successfully completed. It signals the transition from the login phase to the
 * game or session phase.</p>
 */
public class LoginAcknowledgedPacket implements MinecraftPacket {

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction,
                     ProtocolVersion protocolVersion) {
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction,
                     ProtocolVersion protocolVersion) {
  }

  @Override
  public int decodeExpectedMaxLength(ByteBuf buf, ProtocolUtils.Direction direction,
                                     ProtocolVersion version) {
    return 0;
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
