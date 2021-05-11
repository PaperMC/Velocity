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

package com.velocitypowered.proxy.network.pipeline;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketDirection;
import com.velocitypowered.proxy.network.registry.packet.PacketRegistryMap;
import com.velocitypowered.proxy.network.registry.protocol.ProtocolRegistry;
import com.velocitypowered.proxy.network.registry.state.ProtocolStates;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MinecraftEncoder extends MessageToByteEncoder<Packet> {

  private final PacketDirection direction;
  private ProtocolRegistry state;
  private PacketRegistryMap registry;
  private ProtocolVersion version;

  /**
   * Creates a new {@code MinecraftEncoder} encoding packets for the specified {@code direction}.
   *
   * @param direction the direction to encode to
   */
  public MinecraftEncoder(PacketDirection direction) {
    this.direction = Preconditions.checkNotNull(direction, "direction");
    this.state = ProtocolStates.HANDSHAKE;
    this.version = ProtocolVersion.MINIMUM_VERSION;
    this.registry = this.state.lookup(direction, ProtocolVersion.MINIMUM_VERSION);
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, Packet msg, ByteBuf out) {
    this.registry.writePacket(msg, out, this.version);
  }

  public void setProtocolVersion(final ProtocolVersion protocolVersion) {
    this.version = protocolVersion;
    this.registry = this.state.lookup(direction, protocolVersion);
  }

  public void setState(ProtocolRegistry state) {
    this.state = state;
    this.setProtocolVersion(version);
  }
}
