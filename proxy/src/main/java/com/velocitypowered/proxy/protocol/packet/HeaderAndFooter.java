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

import static com.velocitypowered.proxy.protocol.ProtocolUtils.writeString;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public class HeaderAndFooter implements MinecraftPacket {

  private static final String EMPTY_COMPONENT = "{\"translate\":\"\"}";
  private static final HeaderAndFooter RESET = new HeaderAndFooter();

  private final String header;
  private final String footer;

  public HeaderAndFooter() {
    this(EMPTY_COMPONENT, EMPTY_COMPONENT);
  }

  public HeaderAndFooter(String header, String footer) {
    this.header = Preconditions.checkNotNull(header, "header");
    this.footer = Preconditions.checkNotNull(footer, "footer");
  }

  public String getHeader() {
    return header;
  }

  public String getFooter() {
    return footer;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    throw new UnsupportedOperationException("Decode is not implemented");
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    writeString(buf, header);
    writeString(buf, footer);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static HeaderAndFooter create(Component header,
      Component footer, ProtocolVersion protocolVersion) {
    GsonComponentSerializer serializer = ProtocolUtils.getJsonChatSerializer(protocolVersion);
    return new HeaderAndFooter(serializer.serialize(header), serializer.serialize(footer));
  }

  public static HeaderAndFooter reset() {
    return RESET;
  }
}
