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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhases;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.PluginMessagePacket;
import com.velocitypowered.proxy.util.VelocityChannelRegistrar;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import org.junit.jupiter.api.Test;

class ClientPlaySessionHandlerTest {

  @Test
  void forwardsPluginMessageToInFlightServerDuringTransition() {
    VelocityServer server = mock(VelocityServer.class);
    ConnectedPlayer player = mock(ConnectedPlayer.class);
    VelocityServerConnection inFlight = mock(VelocityServerConnection.class);
    MinecraftConnection backend = mock(MinecraftConnection.class);
    VelocityChannelRegistrar channels = mock(VelocityChannelRegistrar.class);
    PluginMessagePacket packet = new PluginMessagePacket(
        "example:channel", Unpooled.buffer().writeByte(1));

    when(server.getChannelRegistrar()).thenReturn(channels);
    when(player.getProtocolVersion()).thenReturn(ProtocolVersion.MINECRAFT_1_20_2);
    when(player.getConnectedServer()).thenReturn(null);
    when(player.getConnectionInFlight()).thenReturn(inFlight);
    when(player.getPhase()).thenReturn(ClientConnectionPhases.VANILLA);
    when(inFlight.getConnection()).thenReturn(backend);
    when(inFlight.getPhase()).thenReturn(BackendConnectionPhases.VANILLA);
    when(backend.getState()).thenReturn(StateRegistry.PLAY);
    doAnswer(invocation -> {
      ReferenceCountUtil.release(invocation.getArgument(0));
      return null;
    }).when(backend).write(any());

    try {
      new ClientPlaySessionHandler(server, player).handle(packet);
      verify(backend).write(packet);
    } finally {
      ReferenceCountUtil.release(packet);
    }
  }
}
