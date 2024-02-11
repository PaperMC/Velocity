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

package com.velocitypowered.proxy.connection.client;

import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.util.VelocityInboundConnection;
import com.velocitypowered.proxy.protocol.packet.LegacyDisconnect;
import com.velocitypowered.proxy.protocol.packet.LegacyPingPacket;
import com.velocitypowered.proxy.protocol.packet.StatusPingPacket;
import com.velocitypowered.proxy.protocol.packet.StatusRequestPacket;
import com.velocitypowered.proxy.protocol.packet.StatusResponsePacket;
import com.velocitypowered.proxy.util.except.QuietRuntimeException;
import io.netty.buffer.ByteBuf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handles server list ping packets from a client.
 */
public class StatusSessionHandler implements MinecraftSessionHandler {

  private static final Logger logger = LogManager.getLogger(StatusSessionHandler.class);
  private static final QuietRuntimeException EXPECTED_AWAITING_REQUEST = new QuietRuntimeException(
      "Expected connection to be awaiting status request");

  private final VelocityServer server;
  private final MinecraftConnection connection;
  private final VelocityInboundConnection inbound;
  private boolean pingReceived = false;

  StatusSessionHandler(VelocityServer server, VelocityInboundConnection inbound) {
    this.server = server;
    this.connection = inbound.getConnection();
    this.inbound = inbound;
  }

  @Override
  public void activated() {
    if (server.getConfiguration().isShowPingRequests()) {
      logger.info("{} is pinging the server with version {}", this.inbound,
          this.connection.getProtocolVersion());
    }
  }

  @Override
  public boolean handle(LegacyPingPacket packet) {
    if (this.pingReceived) {
      throw EXPECTED_AWAITING_REQUEST;
    }
    this.pingReceived = true;
    server.getServerListPingHandler().getInitialPing(this.inbound)
        .thenCompose(ping -> server.getEventManager().fire(new ProxyPingEvent(inbound, ping)))
        .thenAcceptAsync(event -> connection.closeWith(
                LegacyDisconnect.fromServerPing(event.getPing(), packet.getVersion())),
            connection.eventLoop())
        .exceptionally((ex) -> {
          logger.error("Exception while handling legacy ping {}", packet, ex);
          return null;
        });
    return true;
  }

  @Override
  public boolean handle(StatusPingPacket packet) {
    connection.closeWith(packet);
    return true;
  }

  @Override
  public boolean handle(StatusRequestPacket packet) {
    if (this.pingReceived) {
      throw EXPECTED_AWAITING_REQUEST;
    }
    this.pingReceived = true;

    this.server.getServerListPingHandler().getInitialPing(inbound)
        .thenCompose(ping -> server.getEventManager().fire(new ProxyPingEvent(inbound, ping)))
        .thenAcceptAsync(
            (event) -> {
              final StringBuilder json = new StringBuilder();
              VelocityServer.getPingGsonInstance(connection.getProtocolVersion())
                  .toJson(event.getPing(), json);
              connection.write(new StatusResponsePacket(json));
            },
            connection.eventLoop())
        .exceptionally((ex) -> {
          logger.error("Exception while handling status request {}", packet, ex);
          return null;
        });
    return true;
  }

  @Override
  public void handleUnknown(ByteBuf buf) {
    // what even is going on?
    connection.close(true);
  }

  private enum State {
    AWAITING_REQUEST,
    RECEIVED_REQUEST
  }
}
