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

package com.velocitypowered.proxy.protocol.netty;

import com.velocitypowered.proxy.protocol.packet.LegacyDisconnect;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Encodes {@code LegacyDisconnect} for Minecraft 1.3-1.6.4.
 */
@ChannelHandler.Sharable
public class LegacyPingEncoder extends MessageToByteEncoder<LegacyDisconnect> {

  public static final LegacyPingEncoder INSTANCE = new LegacyPingEncoder();

  private LegacyPingEncoder() {
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, LegacyDisconnect msg, ByteBuf out)
      throws Exception {
    out.writeByte(0xff);
    writeLegacyString(out, msg.reason());
  }

  private static void writeLegacyString(ByteBuf out, String string) {
    out.writeShort(string.length());
    out.writeCharSequence(string, StandardCharsets.UTF_16BE);
  }
}
