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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.velocitypowered.proxy.VelocityServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class GameSpyQueryHandlerTest {

  @Mock
  private VelocityServer server;
  @Mock
  private ChannelHandlerContext ctx;

  private final InetSocketAddress sender = new InetSocketAddress("127.0.0.1", 12345);

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void emptyBufferDoesNotThrow() {
    assertDoesNotThrow(() -> handle(Unpooled.EMPTY_BUFFER));
  }

  @Test
  void shortBufferDoesNotThrow() {
    assertDoesNotThrow(() -> handle(Unpooled.buffer().writeBytes(new byte[6])));
  }

  @Test
  void statWithoutChallengeTokenDoesNotThrow() {
    ByteBuf buf = Unpooled.buffer();
    buf.writeByte(0xFE);
    buf.writeByte(0xFD);
    buf.writeByte(0x00);
    buf.writeInt(42);
    assertDoesNotThrow(() -> handle(buf));
  }

  private void handle(ByteBuf buf) throws Exception {
    GameSpyQueryHandler handler = new GameSpyQueryHandler(server);
    DatagramPacket packet = new DatagramPacket(buf, sender, sender);
    handler.channelRead0(ctx, packet);
  }
}
