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
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.text.Component;

/**
 * Represents a packet that contains both the header and footer for the player list screen (tab list) in Minecraft.
 * This packet allows the server to set or update the header and footer text that is displayed on the client's tab list.
 */
public class HeaderAndFooterPacket implements MinecraftPacket {

  private final ComponentHolder header;
  private final ComponentHolder footer;

  public HeaderAndFooterPacket() {
    throw new UnsupportedOperationException("Decode is not implemented");
  }

  public HeaderAndFooterPacket(ComponentHolder header, ComponentHolder footer) {
    this.header = Preconditions.checkNotNull(header, "header");
    this.footer = Preconditions.checkNotNull(footer, "footer");
  }

  public ComponentHolder getHeader() {
    return header;
  }

  public ComponentHolder getFooter() {
    return footer;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    throw new UnsupportedOperationException("Decode is not implemented");
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    header.write(buf);
    footer.write(buf);
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
}
