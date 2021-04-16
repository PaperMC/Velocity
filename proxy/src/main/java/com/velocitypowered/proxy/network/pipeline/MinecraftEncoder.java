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
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.StateRegistry;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketDirection;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MinecraftEncoder extends MessageToByteEncoder<Packet> {

  private final PacketDirection direction;
  private StateRegistry state;
  private StateRegistry.PacketRegistry.ProtocolRegistry registry;

  /**
   * Creates a new {@code MinecraftEncoder} encoding packets for the specified {@code direction}.
   *
   * @param direction the direction to encode to
   */
  public MinecraftEncoder(PacketDirection direction) {
    this.direction = Preconditions.checkNotNull(direction, "direction");
    this.registry = StateRegistry.HANDSHAKE
        .getProtocolRegistry(direction, ProtocolVersion.MINIMUM_VERSION);
    this.state = StateRegistry.HANDSHAKE;
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, Packet msg, ByteBuf out) {
    this.registry.writePacket(msg, out, registry.version);
  }

  public void setProtocolVersion(final ProtocolVersion protocolVersion) {
    this.registry = state.getProtocolRegistry(direction, protocolVersion);
  }

  public void setState(StateRegistry state) {
    this.state = state;
    this.setProtocolVersion(registry.version);
  }
}
