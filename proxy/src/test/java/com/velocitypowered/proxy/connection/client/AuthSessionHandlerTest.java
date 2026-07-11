/*
 * Copyright (C) 2026 Velocity Contributors
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.network.HandshakeIntent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.ConnectionTypes;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.event.MockEventManager;
import com.velocitypowered.proxy.protocol.StateRegistry;
import io.netty.channel.EventLoop;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthSessionHandlerTest {

  @Test
  void duplicateLoginRejectedBeforeOwnershipDoesNotTeardown() {
    TestHarness harness = new TestHarness();
    when(harness.server.canRegisterConnection(any())).thenReturn(false);

    harness.handler.activated();
    harness.handler.disconnected();

    verify(harness.server, never()).unregisterConnection(any());
    verify(harness.inbound).cleanup();
  }

  @Test
  void disconnectedWithOwnedPlayerStillTeardowns() throws ReflectiveOperationException {
    TestHarness harness = new TestHarness();
    ConnectedPlayer player = mock(ConnectedPlayer.class);
    setConnectedPlayer(harness.handler, player);

    harness.handler.disconnected();

    verify(player).teardown();
    verify(harness.inbound).cleanup();
  }

  private static void setConnectedPlayer(AuthSessionHandler handler, ConnectedPlayer player)
      throws ReflectiveOperationException {
    Field field = AuthSessionHandler.class.getDeclaredField("connectedPlayer");
    field.setAccessible(true);
    field.set(handler, player);
  }

  private static EventLoop immediateEventLoop() {
    EventLoop eventLoop = mock(EventLoop.class);
    doAnswer(invocation -> {
      ((Runnable) invocation.getArgument(0)).run();
      return null;
    }).when(eventLoop).execute(any(Runnable.class));
    return eventLoop;
  }

  private static final class TestHarness {

    private final VelocityServer server = mock(VelocityServer.class);
    private final VelocityConfiguration configuration = mock(VelocityConfiguration.class);
    private final MockEventManager eventManager = new MockEventManager();
    private final LoginInboundConnection inbound = mock(LoginInboundConnection.class);
    private final MinecraftConnection connection = mock(MinecraftConnection.class);
    private final AuthSessionHandler handler;

    private TestHarness() {
      EventLoop eventLoop = immediateEventLoop();
      when(server.getConfiguration()).thenReturn(configuration);
      when(server.getEventManager()).thenReturn(eventManager);
      when(server.getPlayer(any(UUID.class))).thenReturn(Optional.empty());
      when(configuration.getPlayerInfoForwardingMode()).thenReturn(PlayerInfoForwarding.NONE);
      when(configuration.isLogPlayerConnections()).thenReturn(false);

      when(inbound.delegatedConnection()).thenReturn(connection);
      when(inbound.getVirtualHost()).thenReturn(Optional.empty());
      when(inbound.getRawVirtualHost()).thenReturn(Optional.empty());
      when(inbound.getHandshakeIntent()).thenReturn(HandshakeIntent.LOGIN);
      when(inbound.getIdentifiedKey()).thenReturn(null);

      when(connection.getType()).thenReturn(ConnectionTypes.VANILLA);
      when(connection.getProtocolVersion()).thenReturn(ProtocolVersion.MINECRAFT_1_20_2);
      when(connection.eventLoop()).thenReturn(eventLoop);
      when(connection.isClosed()).thenReturn(false);
      when(connection.getState()).thenReturn(StateRegistry.LOGIN);

      handler = new AuthSessionHandler(
          server,
          inbound,
          GameProfile.forOfflinePlayer("test-player"),
          false,
          null);
    }
  }
}
