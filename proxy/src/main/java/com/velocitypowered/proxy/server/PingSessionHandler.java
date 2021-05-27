/*
 * Copyright (C) 2018 Velocity Contributors
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

package com.velocitypowered.proxy.server;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundStatusResponsePacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundHandshakePacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundStatusRequestPacket;
import com.velocitypowered.proxy.network.registry.state.ProtocolStates;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

public class PingSessionHandler implements MinecraftSessionHandler {

  private final CompletableFuture<ServerPing> result;
  private final RegisteredServer server;
  private final MinecraftConnection connection;
  private final ProtocolVersion version;
  private boolean completed = false;

  PingSessionHandler(CompletableFuture<ServerPing> result, RegisteredServer server,
      MinecraftConnection connection, ProtocolVersion version) {
    this.result = result;
    this.server = server;
    this.connection = connection;
    this.version = version;
  }

  @Override
  public void activated() {
    SocketAddress address = server.serverInfo().address();
    String hostname;
    int port;
    if (address instanceof InetSocketAddress) {
      InetSocketAddress socketAddr = (InetSocketAddress) address;
      hostname = socketAddr.getHostString();
      port = socketAddr.getPort();
    } else {
      // Just fake it
      hostname = "127.0.0.1";
      port = 25565;
    }

    connection.delayedWrite(new ServerboundHandshakePacket(
        version,
        hostname,
        port,
        ServerboundHandshakePacket.STATUS_ID
    ));

    connection.setState(ProtocolStates.STATUS);
    connection.delayedWrite(ServerboundStatusRequestPacket.INSTANCE);

    connection.flush();
  }

  @Override
  public boolean handle(ClientboundStatusResponsePacket packet) {
    // All good!
    completed = true;
    connection.close(true);

    ServerPing ping = VelocityServer.getPingGsonInstance(version).fromJson(packet.getStatus(),
        ServerPing.class);
    result.complete(ping);
    return true;
  }

  @Override
  public void disconnected() {
    if (!completed) {
      result.completeExceptionally(new IOException("Unexpectedly disconnected from remote server"));
    }
  }

  @Override
  public void exception(Throwable throwable) {
    completed = true;
    result.completeExceptionally(throwable);
  }
}
