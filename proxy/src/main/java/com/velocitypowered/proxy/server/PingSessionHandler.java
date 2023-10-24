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

package com.velocitypowered.proxy.server;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.Handshake;
import com.velocitypowered.proxy.protocol.packet.StatusRequest;
import com.velocitypowered.proxy.protocol.packet.StatusResponse;
import io.netty.channel.EventLoop;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Session handler used to implement {@link VelocityRegisteredServer#ping(EventLoop,
 * ProtocolVersion)}.
 */
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
    Handshake handshake = new Handshake();
    handshake.setNextStatus(StateRegistry.STATUS_ID);
    handshake.setServerAddress(server.getServerInfo().getAddress().getHostString());
    handshake.setPort(server.getServerInfo().getAddress().getPort());
    handshake.setProtocolVersion(version);
    connection.delayedWrite(handshake);

    connection.setActiveSessionHandler(StateRegistry.STATUS);
    connection.setState(StateRegistry.STATUS);
    connection.delayedWrite(StatusRequest.INSTANCE);

    connection.flush();
  }

  @Override
  public boolean handle(StatusResponse packet) {
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
