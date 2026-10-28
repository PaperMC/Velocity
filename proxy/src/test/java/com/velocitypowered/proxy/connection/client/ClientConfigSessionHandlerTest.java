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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhase;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.event.VelocityEventManager;
import com.velocitypowered.proxy.protocol.packet.ServerboundCustomClickActionPacket;
import io.netty.channel.EventLoop;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ClientConfigSessionHandlerTest {

  private VelocityServer server;
  private VelocityEventManager eventManager;
  private ConnectedPlayer player;
  private MinecraftConnection playerConnection;
  private ClientConfigSessionHandler handler;

  @BeforeEach
  void setUp() {
    server = mock(VelocityServer.class);
    eventManager = mock(VelocityEventManager.class);
    when(server.getEventManager()).thenReturn(eventManager);
    when(eventManager.fire(any())).thenAnswer(inv ->
        CompletableFuture.completedFuture(inv.getArgument(0)));
    player = mock(ConnectedPlayer.class);
    playerConnection = mock(MinecraftConnection.class);
    when(player.getConnection()).thenReturn(playerConnection);
    EventLoop mockEventLoop = mock(EventLoop.class, invocation -> {
      invocation.getArgument(0, Runnable.class).run();
      return null;
    });
    when(playerConnection.eventLoop()).thenReturn(mockEventLoop);
    handler = new ClientConfigSessionHandler(server, player);
  }

  @AfterEach
  void tearDown() {
    // nothing to clean up; each test manages its own ByteBufs
  }

  private ServerboundCustomClickActionPacket makePacket() {
    return new ServerboundCustomClickActionPacket(Key.key("velocity", "test"));
  }

  @Test
  void handleForwardsToInFlightServer() {
    VelocityServerConnection inFlight = mock(VelocityServerConnection.class);
    MinecraftConnection backend = mock(MinecraftConnection.class);
    when(player.getConnectionInFlightOrConnectedServer()).thenReturn(inFlight);
    when(inFlight.getPlayer()).thenReturn(player);
    when(inFlight.getConnection()).thenReturn(backend);
    when(inFlight.ensureConnected()).thenReturn(backend);

    ServerboundCustomClickActionPacket pkt = makePacket();
    assertTrue(handler.handle(pkt));
    ArgumentCaptor<ServerboundCustomClickActionPacket> captor = ArgumentCaptor
        .forClass(ServerboundCustomClickActionPacket.class);
    verify(backend).write(captor.capture());
    ServerboundCustomClickActionPacket sent = captor.getValue();
    assertEquals(pkt.id(), sent.id());
    assertEquals(pkt.payload(), sent.payload());
  }

  @Test
  void handleForwardsToConnectedServerWhenInFlightIsNull() {
    VelocityServerConnection connected = mock(VelocityServerConnection.class);
    MinecraftConnection backend = mock(MinecraftConnection.class);
    when(player.getConnectionInFlightOrConnectedServer()).thenReturn(connected);
    when(connected.getPlayer()).thenReturn(player);
    when(connected.getConnection()).thenReturn(backend);
    when(connected.ensureConnected()).thenReturn(backend);

    ServerboundCustomClickActionPacket pkt = makePacket();
    assertTrue(handler.handle(pkt));
    ArgumentCaptor<ServerboundCustomClickActionPacket> captor = ArgumentCaptor
        .forClass(ServerboundCustomClickActionPacket.class);
    verify(backend).write(captor.capture());
    ServerboundCustomClickActionPacket sent = captor.getValue();
    assertEquals(pkt.id(), sent.id());
    assertEquals(pkt.payload(), sent.payload());
  }

  @Test
  void handleReturnsFalseWhenNoServer() {
    when(player.getConnectionInFlightOrConnectedServer()).thenReturn(null);

    ServerboundCustomClickActionPacket pkt = makePacket();
    assertFalse(handler.handle(pkt));
  }

  @Test
  void handleGenericForwards() {
    VelocityServerConnection connected = mock(VelocityServerConnection.class);
    MinecraftConnection backend = mock(MinecraftConnection.class);
    BackendConnectionPhase phase = mock(BackendConnectionPhase.class);
    when(player.getConnectedServer()).thenReturn(connected);
    when(connected.getConnection()).thenReturn(backend);
    when(connected.getPhase()).thenReturn(phase);
    when(phase.consideredComplete()).thenReturn(true);

    ServerboundCustomClickActionPacket pkt = makePacket();

    handler.handleGeneric(pkt);

    verify(backend).write(pkt);
  }
}
