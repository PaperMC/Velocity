/*
 * Copyright (C) 2018-2026 Velocity Contributors
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

package com.velocitypowered.proxy.connection.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhase;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.protocol.packet.ServerboundCustomClickActionPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import org.junit.jupiter.api.Test;

class ClientConfigSessionHandlerTest {

  @Test
  void forwardsCustomClickActionWithoutOverReleasingFrame() {
    final VelocityServer server = mock(VelocityServer.class);
    ConnectedPlayer player = mock(ConnectedPlayer.class);
    VelocityServerConnection serverConnection = mock(VelocityServerConnection.class);
    BackendConnectionPhase phase = mock(BackendConnectionPhase.class);
    MinecraftConnection backendConnection = mock(MinecraftConnection.class);
    when(player.getConnectionInFlightOrConnectedServer()).thenReturn(serverConnection);
    when(player.getConnectedServer()).thenReturn(serverConnection);
    when(serverConnection.ensureConnected()).thenReturn(backendConnection);
    when(serverConnection.getConnection()).thenReturn(backendConnection);
    when(serverConnection.getPhase()).thenReturn(phase);
    when(phase.consideredComplete()).thenReturn(true);
    doAnswer(invocation -> {
      ReferenceCountUtil.release(invocation.getArgument(0));
      return null;
    }).when(backendConnection).write(any());

    ByteBuf frame = Unpooled.buffer().writeByte(0);
    ServerboundCustomClickActionPacket packet = new ServerboundCustomClickActionPacket();
    packet.replace(frame.readRetainedSlice(frame.readableBytes()));
    ClientConfigSessionHandler handler = new ClientConfigSessionHandler(server, player);
    try {
      assertDoesNotThrow(() -> {
        try {
          if (!packet.handle(handler)) {
            handler.handleGeneric(packet);
          }
        } finally {
          ReferenceCountUtil.release(packet);
          frame.release();
        }
      });
      assertEquals(0, frame.refCnt());
      verify(backendConnection).write(same(packet));
    } finally {
      if (frame.refCnt() > 0) {
        frame.release(frame.refCnt());
      }
    }
  }
}
