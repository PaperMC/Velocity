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

package com.velocitypowered.proxy.protocol.packet;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.PacketCodec;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.text.Component;

public record HeaderAndFooterPacket(ComponentHolder header,
    ComponentHolder footer) implements MinecraftPacket {

  public HeaderAndFooterPacket {
    Preconditions.checkNotNull(header, "header");
    Preconditions.checkNotNull(footer, "footer");
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static HeaderAndFooterPacket create(Component header,
                                             Component footer, ProtocolVersion protocolVersion) {
    return new HeaderAndFooterPacket(new ComponentHolder(protocolVersion, header),
      new ComponentHolder(protocolVersion, footer));
  }

  public static HeaderAndFooterPacket reset(ProtocolVersion version) {
    ComponentHolder empty = new ComponentHolder(version, Component.empty());
    return new HeaderAndFooterPacket(empty, empty);
  }

  public static class Codec implements PacketCodec<HeaderAndFooterPacket> {
    @Override
    public HeaderAndFooterPacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
        ProtocolVersion protocolVersion) {
      throw new UnsupportedOperationException("Decode is not implemented");
    }

    @Override
    public void encode(HeaderAndFooterPacket packet, ByteBuf buf,
        ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
      packet.header.write(buf);
      packet.footer.write(buf);
    }
  }
}
