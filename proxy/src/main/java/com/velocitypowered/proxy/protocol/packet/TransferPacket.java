/*
 * Copyright (C) 2024 Velocity Contributors
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
import java.net.InetSocketAddress;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a packet used to transfer a player to another server.
 */
public class TransferPacket implements MinecraftPacket {
  private String host;
  private int port;

  public TransferPacket() {
  }

  /**
   * Constructs a {@code TransferPacket} with the specified host and port.
   *
   * @param host the hostname of the destination server
   * @param port the port of the destination server
   */
  public TransferPacket(final String host, final int port) {
    this.host = host;
    this.port = port;
  }

  /**
   * Gets the {@link InetSocketAddress} representing the transfer address.
   *
   * @return the {@code InetSocketAddress}, or {@code null} if the host is not set
   */
  @Nullable
  public InetSocketAddress address() {
    if (host == null) {
      return null;
    }
    return new InetSocketAddress(host, port);
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    this.host = ProtocolUtils.readString(buf);
    this.port = ProtocolUtils.readVarInt(buf);
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeString(buf, host);
    ProtocolUtils.writeVarInt(buf, port);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
