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
import static com.velocitypowered.proxy.protocol.util.NettyPreconditions.checkFrame;

import com.google.common.collect.ImmutableMap;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public class TransferPacket implements MinecraftPacket {
  private String host;
  private int port;
  private Map<String, String> properties = Map.of(); // 26.4+

  public TransferPacket() {
  }

  public TransferPacket(final String host, final int port) {
    this(host, port, Map.of());
  }

  public TransferPacket(final String host, final int port, final Map<String, String> properties) {
    this.host = host;
    this.port = port;
    this.properties = ImmutableMap.copyOf(properties);
  }

  /**
   * Returns the properties the client appends to the handshake host of its next connection
   * (26.4+, empty for older clients).
   *
   * @return the transfer properties
   */
  public Map<String, String> properties() {
    return properties;
  }

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
    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_26_4)) {
      int size = ProtocolUtils.readVarInt(buf);
      checkFrame(size >= 0 && size <= buf.readableBytes(),
          "Bad transfer property count (%s)", size);
      Map<String, String> properties = new LinkedHashMap<>(size);
      for (int i = 0; i < size; i++) {
        properties.put(ProtocolUtils.readString(buf), ProtocolUtils.readString(buf));
      }
      this.properties = ImmutableMap.copyOf(properties);
    } else {
      this.properties = Map.of();
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeString(buf, host);
    ProtocolUtils.writeVarInt(buf, port);
    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_26_4)) {
      ProtocolUtils.writeVarInt(buf, properties.size());
      for (Map.Entry<String, String> property : properties.entrySet()) {
        ProtocolUtils.writeString(buf, property.getKey());
        ProtocolUtils.writeString(buf, property.getValue());
      }
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
