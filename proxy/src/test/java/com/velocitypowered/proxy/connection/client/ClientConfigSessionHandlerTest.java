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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClientConfigSessionHandlerTest {

  private VelocityServer server;
  private ConnectedPlayer player;
  private ClientConfigSessionHandler handler;

  @BeforeEach
  void setUp() {
    server = mock(VelocityServer.class);
    player = mock(ConnectedPlayer.class);
    handler = new ClientConfigSessionHandler(server, player);
  }

  @AfterEach
  void tearDown() {
    // nothing to clean up; each test manages its own ByteBufs
  }

  private ServerboundCustomClickActionPacket makePacket() {
    ByteBuf frame = Unpooled.buffer().writeByte(0);
    ServerboundCustomClickActionPacket pkt = new ServerboundCustomClickActionPacket();
    pkt.replace(frame.readRetainedSlice(frame.readableBytes()));
    return pkt;
  }

  @Test
  void handleForwardsToInFlightServer() {
    VelocityServerConnection inFlight = mock(VelocityServerConnection.class);
    MinecraftConnection backend = mock(MinecraftConnection.class);
    when(player.getConnectionInFlightOrConnectedServer()).thenReturn(inFlight);
    when(inFlight.ensureConnected()).thenReturn(backend);

    ServerboundCustomClickActionPacket pkt = makePacket();
    assertTrue(handler.handle(pkt));
    verify(backend).write(pkt);
    ReferenceCountUtil.release(pkt);
  }

  @Test
  void handleForwardsToConnectedServerWhenInFlightIsNull() {
    VelocityServerConnection connected = mock(VelocityServerConnection.class);
    MinecraftConnection backend = mock(MinecraftConnection.class);
    when(player.getConnectionInFlightOrConnectedServer()).thenReturn(connected);
    when(connected.ensureConnected()).thenReturn(backend);

    ServerboundCustomClickActionPacket pkt = makePacket();
    assertTrue(handler.handle(pkt));
    verify(backend).write(pkt);
    ReferenceCountUtil.release(pkt);
  }

  @Test
  void handleReturnsFalseWhenNoServer() {
    when(player.getConnectionInFlightOrConnectedServer()).thenReturn(null);

    ServerboundCustomClickActionPacket pkt = makePacket();
    assertFalse(handler.handle(pkt));
    ReferenceCountUtil.release(pkt);
  }

  @Test
  void handleGenericRetainsAndForwards() {
    VelocityServerConnection connected = mock(VelocityServerConnection.class);
    MinecraftConnection backend = mock(MinecraftConnection.class);
    BackendConnectionPhase phase = mock(BackendConnectionPhase.class);
    when(player.getConnectedServer()).thenReturn(connected);
    when(connected.getConnection()).thenReturn(backend);
    when(connected.getPhase()).thenReturn(phase);
    when(phase.consideredComplete()).thenReturn(true);

    ServerboundCustomClickActionPacket pkt = makePacket();
    int refBefore = pkt.refCnt();

    handler.handleGeneric(pkt);

    // retain() was called (+1) before write
    assertEquals(refBefore + 1, pkt.refCnt());
    verify(backend).write(pkt);
    ReferenceCountUtil.release(pkt);
  }
}
