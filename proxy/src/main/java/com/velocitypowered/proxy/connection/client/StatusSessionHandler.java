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

package com.velocitypowered.proxy.connection.client;

import com.google.common.collect.ImmutableList;
import com.spotify.futures.CompletableFutures;
import com.velocitypowered.api.event.connection.ProxyPingEventImpl;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.connection.InboundConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.ModInfo;
import com.velocitypowered.api.util.ProxyVersion;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PingPassthroughMode;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundStatusPingPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundStatusResponsePacket;
import com.velocitypowered.proxy.network.packet.legacy.LegacyDisconnectPacket;
import com.velocitypowered.proxy.network.packet.legacy.LegacyPingPacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundStatusPingPacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundStatusRequestPacket;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import com.velocitypowered.proxy.util.except.QuietRuntimeException;
import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StatusSessionHandler implements MinecraftSessionHandler {

  private static final Logger logger = LogManager.getLogger(StatusSessionHandler.class);
  private static final QuietRuntimeException EXPECTED_AWAITING_REQUEST = new QuietRuntimeException(
      "Expected connection to be awaiting status request");

  private final VelocityServer server;
  private final MinecraftConnection connection;
  private final InboundConnection inbound;
  private boolean pingReceived = false;

  StatusSessionHandler(VelocityServer server, MinecraftConnection connection,
      InboundConnection inbound) {
    this.server = server;
    this.connection = connection;
    this.inbound = inbound;
  }

  @Override
  public void activated() {
    if (server.configuration().isShowPingRequests()) {
      logger.info("{} is pinging the server with version {}", this.inbound,
          this.connection.getProtocolVersion());
    }
  }

  private ServerPing constructLocalPing(ProtocolVersion version) {
    VelocityConfiguration configuration = server.configuration();
    ProxyVersion proxyVersion = server.version();
    return new ServerPing(
        new ServerPing.Version(version.protocol(),
            proxyVersion.name() + " " + ProtocolVersion.SUPPORTED_VERSION_STRING),
        new ServerPing.Players(server.countConnectedPlayers(), configuration.getShowMaxPlayers(),
            ImmutableList.of()),
        configuration.getMotd(),
        configuration.getFavicon(),
        configuration.isAnnounceForge() ? ModInfo.DEFAULT : null
    );
  }

  private CompletableFuture<ServerPing> attemptPingPassthrough(PingPassthroughMode mode,
      List<String> servers, ProtocolVersion pingingVersion) {
    ServerPing fallback = constructLocalPing(pingingVersion);
    List<CompletableFuture<ServerPing>> pings = new ArrayList<>();
    for (String s : servers) {
      RegisteredServer rs = server.server(s);
      if (rs == null) {
        continue;
      }
      VelocityRegisteredServer vrs = (VelocityRegisteredServer) rs;
      pings.add(vrs.ping(connection.eventLoop(), pingingVersion));
    }
    if (pings.isEmpty()) {
      return CompletableFuture.completedFuture(fallback);
    }

    CompletableFuture<List<ServerPing>> pingResponses = CompletableFutures.successfulAsList(pings,
        (ex) -> fallback);
    switch (mode) {
      case ALL:
        return pingResponses.thenApply(responses -> {
          // Find the first non-fallback
          for (ServerPing response : responses) {
            if (response == fallback) {
              continue;
            }
            return response;
          }
          return fallback;
        });
      case MODS:
        return pingResponses.thenApply(responses -> {
          // Find the first non-fallback that contains a mod list
          for (ServerPing response : responses) {
            if (response == fallback) {
              continue;
            }
            ModInfo modInfo = response.modInfo();
            if (modInfo != null) {
              return fallback.asBuilder().mods(modInfo).build();
            }
          }
          return fallback;
        });
      case DESCRIPTION:
        return pingResponses.thenApply(responses -> {
          // Find the first non-fallback. If it includes a modlist, add it too.
          for (ServerPing response : responses) {
            if (response == fallback) {
              continue;
            }

            if (response.description() == null) {
              continue;
            }

            return new ServerPing(
                fallback.version(),
                fallback.players(),
                response.description(),
                fallback.favicon(),
                response.modInfo()
            );
          }
          return fallback;
        });
      default:
        // Not possible, but covered for completeness.
        return CompletableFuture.completedFuture(fallback);
    }
  }

  private CompletableFuture<ServerPing> getInitialPing() {
    VelocityConfiguration configuration = server.configuration();
    ProtocolVersion shownVersion = ProtocolVersion.isSupported(connection.getProtocolVersion())
        ? connection.getProtocolVersion() : ProtocolVersion.MAXIMUM_VERSION;
    PingPassthroughMode passthrough = configuration.getPingPassthrough();

    if (passthrough == PingPassthroughMode.DISABLED) {
      return CompletableFuture.completedFuture(constructLocalPing(shownVersion));
    } else {
      InetSocketAddress vhost = inbound.connectedHostname();
      String virtualHostStr = vhost == null ? "" : vhost.getHostString().toLowerCase(Locale.ROOT);
      List<String> serversToTry = server.configuration().getForcedHosts().getOrDefault(
          virtualHostStr, server.configuration().getAttemptConnectionOrder());
      return attemptPingPassthrough(configuration.getPingPassthrough(), serversToTry, shownVersion);
    }
  }

  @Override
  public boolean handle(LegacyPingPacket packet) {
    if (this.pingReceived) {
      throw EXPECTED_AWAITING_REQUEST;
    }
    this.pingReceived = true;
    getInitialPing()
        .thenCompose(ping -> server.eventManager().fire(new ProxyPingEventImpl(inbound, ping)))
        .thenAcceptAsync(event -> connection.closeWith(
            LegacyDisconnectPacket.fromServerPing(event.ping(), packet.getVersion())),
            connection.eventLoop())
        .exceptionally((ex) -> {
          logger.error("Exception while handling legacy ping {}", packet, ex);
          return null;
        });
    return true;
  }

  @Override
  public boolean handle(ServerboundStatusPingPacket packet) {
    connection.closeWith(new ClientboundStatusPingPacket(packet.getRandomId()));
    return true;
  }

  @Override
  public boolean handle(ServerboundStatusRequestPacket packet) {
    if (this.pingReceived) {
      throw EXPECTED_AWAITING_REQUEST;
    }
    this.pingReceived = true;

    getInitialPing()
        .thenCompose(ping -> server.eventManager().fire(new ProxyPingEventImpl(inbound, ping)))
        .thenAcceptAsync(
            (event) -> {
              StringBuilder json = new StringBuilder();
              VelocityServer.getPingGsonInstance(connection.getProtocolVersion())
                  .toJson(event.ping(), json);
              connection.write(new ClientboundStatusResponsePacket(json));
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
}
