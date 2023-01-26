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

package com.velocitypowered.proxy.connection.util;

import com.google.common.collect.ImmutableList;
import com.spotify.futures.CompletableFutures;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.server.PingOptions;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.ModInfo;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PingPassthroughMode;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Common utilities for handling server list ping results.
 */
public class ServerListPingHandler {

  private final VelocityServer server;

  public ServerListPingHandler(VelocityServer server) {
    this.server = server;
  }

  private ServerPing constructLocalPing(ProtocolVersion version) {
    VelocityConfiguration configuration = server.getConfiguration();
    return new ServerPing(
        new ServerPing.Version(version.getProtocol(),
            "Velocity " + ProtocolVersion.SUPPORTED_VERSION_STRING),
        new ServerPing.Players(server.getPlayerCount(), configuration.getShowMaxPlayers(),
            ImmutableList.of()),
        configuration.getMotd(),
        configuration.getFavicon().orElse(null),
        configuration.isAnnounceForge() ? ModInfo.DEFAULT : null
    );
  }

  private CompletableFuture<ServerPing> attemptPingPassthrough(VelocityInboundConnection connection,
      PingPassthroughMode mode, List<String> servers, ProtocolVersion responseProtocolVersion) {
    ServerPing fallback = constructLocalPing(connection.getProtocolVersion());
    List<CompletableFuture<ServerPing>> pings = new ArrayList<>();
    for (String s : servers) {
      Optional<RegisteredServer> rs = server.getServer(s);
      if (rs.isEmpty()) {
        continue;
      }
      VelocityRegisteredServer vrs = (VelocityRegisteredServer) rs.get();
      pings.add(vrs.ping(connection.getConnection().eventLoop(), PingOptions.builder()
              .version(responseProtocolVersion).build()));
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
            Optional<ModInfo> modInfo = response.getModinfo();
            if (modInfo.isPresent()) {
              return fallback.asBuilder().mods(modInfo.get()).build();
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

            if (response.getDescriptionComponent() == null) {
              continue;
            }

            return new ServerPing(
                fallback.getVersion(),
                fallback.getPlayers().orElse(null),
                response.getDescriptionComponent(),
                fallback.getFavicon().orElse(null),
                response.getModinfo().orElse(null)
            );
          }
          return fallback;
        });
      // Not possible, but covered for completeness.
      default:
        return CompletableFuture.completedFuture(fallback);
    }
  }

  /**
   * Fetches the "default" server ping for a player.
   *
   * @param connection the connection
   * @return a future with the initial ping result
   */
  public CompletableFuture<ServerPing> getInitialPing(VelocityInboundConnection connection) {
    VelocityConfiguration configuration = server.getConfiguration();
    ProtocolVersion shownVersion = ProtocolVersion.isSupported(connection.getProtocolVersion())
        ? connection.getProtocolVersion() : ProtocolVersion.MAXIMUM_VERSION;
    PingPassthroughMode passthroughMode = configuration.getPingPassthrough();

    if (passthroughMode == PingPassthroughMode.DISABLED) {
      return CompletableFuture.completedFuture(constructLocalPing(shownVersion));
    } else {
      String virtualHostStr = connection.getVirtualHost().map(InetSocketAddress::getHostString)
          .map(str -> str.toLowerCase(Locale.ROOT))
          .orElse("");
      List<String> serversToTry = server.getConfiguration().getForcedHosts().getOrDefault(
          virtualHostStr, server.getConfiguration().getAttemptConnectionOrder());
      return attemptPingPassthrough(connection, passthroughMode, serversToTry, shownVersion);
    }
  }
}
