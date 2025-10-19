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
import io.netty.buffer.ByteBuf;

public final class TitleTimesPacket extends GenericTitlePacket {

  private final int fadeIn;
  private final int stay;
  private final int fadeOut;

  public TitleTimesPacket(int fadeIn, int stay, int fadeOut) {
    super(ActionType.SET_TIMES);
    this.fadeIn = fadeIn;
    this.stay = stay;
    this.fadeOut = fadeOut;
  }

  @Override
  public int getFadeIn() {
    return fadeIn;
  }

  @Override
  public int getStay() {
    return stay;
  }

  @Override
  public int getFadeOut() {
    return fadeOut;
  }

  @Override
  public String toString() {
    return "TitleTimesPacket{"
        + "fadeIn=" + fadeIn
        + ", stay=" + stay
        + ", fadeOut=" + fadeOut
        + '}';
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static class Codec implements PacketCodec<TitleTimesPacket> {
    @Override
    public TitleTimesPacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
        ProtocolVersion protocolVersion) {
      throw new UnsupportedOperationException(); // encode only
    }

    @Override
    public void encode(TitleTimesPacket packet, ByteBuf buf, ProtocolUtils.Direction direction,
        ProtocolVersion protocolVersion) {
      buf.writeInt(packet.fadeIn);
      buf.writeInt(packet.stay);
      buf.writeInt(packet.fadeOut);
    }
  }
}
