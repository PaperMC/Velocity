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

package com.velocitypowered.proxy.network.packet.clientbound;

import static com.velocitypowered.proxy.network.ProtocolUtils.writeString;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.PacketWriter;
import io.netty.buffer.ByteBuf;

public class ClientboundHeaderAndFooterPacket implements Packet {
  public static final PacketReader<ClientboundHeaderAndFooterPacket> DECODER = PacketReader.unsupported();
  public static final PacketWriter<ClientboundHeaderAndFooterPacket> ENCODER = (out, packet, version) -> {
    writeString(out, packet.header);
    writeString(out, packet.footer);
  };

  private static final String EMPTY_COMPONENT = "{\"translate\":\"\"}";
  private static final ClientboundHeaderAndFooterPacket RESET
      = new ClientboundHeaderAndFooterPacket();

  private final String header;
  private final String footer;

  public ClientboundHeaderAndFooterPacket() {
    this(EMPTY_COMPONENT, EMPTY_COMPONENT);
  }

  public ClientboundHeaderAndFooterPacket(String header, String footer) {
    this.header = Preconditions.checkNotNull(header, "header");
    this.footer = Preconditions.checkNotNull(footer, "footer");
  }

  @Override
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
  }

  public String getHeader() {
    return header;
  }

  public String getFooter() {
    return footer;
  }

  public static ClientboundHeaderAndFooterPacket reset() {
    return RESET;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("header", this.header)
      .add("footer", this.footer)
      .toString();
  }
}
