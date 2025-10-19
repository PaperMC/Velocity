/*
 * Copyright (C) 2018-2021 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet.title;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.PacketCodec;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.netty.buffer.ByteBuf;

public final class TitleSubtitlePacket extends GenericTitlePacket {

  private final ComponentHolder component;

  public TitleSubtitlePacket(ComponentHolder component) {
    super(ActionType.SET_SUBTITLE);
    this.component = component;
  }

  @Override
  public ComponentHolder getComponent() {
    return component;
  }

  @Override
  public String toString() {
    return "TitleSubtitlePacket{"
        + "component='" + component + '\''
        + '}';
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static class Codec implements PacketCodec<TitleSubtitlePacket> {
    @Override
    public TitleSubtitlePacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
        ProtocolVersion protocolVersion) {
      throw new UnsupportedOperationException(); // encode only
    }

    @Override
    public void encode(TitleSubtitlePacket packet, ByteBuf buf, ProtocolUtils.Direction direction,
        ProtocolVersion protocolVersion) {
      packet.component.write(buf);
    }
  }
}
