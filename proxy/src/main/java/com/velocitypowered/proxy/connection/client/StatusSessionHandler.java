package com.velocitypowered.proxy.connection.client;

import com.google.common.collect.ImmutableList;
import com.spotify.futures.CompletableFutures;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.ModInfo;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PingPassthroughMode;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.packet.LegacyDisconnect;
import com.velocitypowered.proxy.protocol.packet.LegacyPing;
import com.velocitypowered.proxy.protocol.packet.StatusPing;
import com.velocitypowered.proxy.protocol.packet.StatusRequest;
import com.velocitypowered.proxy.protocol.packet.StatusResponse;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StatusSessionHandler implements MinecraftSessionHandler {

  private static final Logger logger = LogManager.getLogger(StatusSessionHandler.class);

  private final VelocityServer server;
  private final MinecraftConnection connection;
  private final InboundConnection inbound;

  StatusSessionHandler(VelocityServer server, MinecraftConnection connection,
      InboundConnection inbound) {
    this.server = server;
    this.connection = connection;
    this.inbound = inbound;
  }

  @Override
  public void activated() {
    if (server.getConfiguration().isShowPingRequests()) {
      logger.info("{} is pinging the server with version {}", this.inbound,
          this.connection.getProtocolVersion());
    }
  }

  private ServerPing constructLocalPing(ProtocolVersion version) {
    VelocityConfiguration configuration = server.getConfiguration();
    return new ServerPing(
        new ServerPing.Version(version.getProtocol(),
            "Velocity " + ProtocolVersion.SUPPORTED_VERSION_STRING),
        new ServerPing.Players(server.getPlayerCount(), configuration.getShowMaxPlayers(),
            ImmutableList.of()),
        configuration.getMotdComponent(),
        configuration.getFavicon().orElse(null),
        configuration.isAnnounceForge() ? ModInfo.DEFAULT : null
    );
  }

  private CompletableFuture<ServerPing> attemptPingPassthrough(PingPassthroughMode mode,
      List<String> servers, ProtocolVersion pingingVersion) {
    ServerPing fallback = constructLocalPing(pingingVersion);
    List<CompletableFuture<ServerPing>> pings = new ArrayList<>();
    for (String s : servers) {
      Optional<RegisteredServer> rs = server.getServer(s);
      if (!rs.isPresent()) {
        continue;
      }
      VelocityRegisteredServer vrs = (VelocityRegisteredServer) rs.get();
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

            return new ServerPing(
                fallback.getVersion(),
                fallback.getPlayers().orElse(null),
                response.getDescription(),
                fallback.getFavicon().orElse(null),
                response.getModinfo().orElse(null)
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
    VelocityConfiguration configuration = server.getConfiguration();
    ProtocolVersion shownVersion = ProtocolVersion.isSupported(connection.getProtocolVersion())
        ? connection.getProtocolVersion() : ProtocolVersion.MAXIMUM_VERSION;
    PingPassthroughMode passthrough = configuration.getPingPassthrough();

    if (passthrough == PingPassthroughMode.DISABLED) {
      return CompletableFuture.completedFuture(constructLocalPing(shownVersion));
    } else {
      String virtualHostStr = inbound.getVirtualHost().map(InetSocketAddress::getHostString)
          .orElse("");
      List<String> serversToTry = server.getConfiguration().getForcedHosts().getOrDefault(
          virtualHostStr, server.getConfiguration().getAttemptConnectionOrder());
      return attemptPingPassthrough(configuration.getPingPassthrough(), serversToTry, shownVersion);
    }
  }

  @Override
  public boolean handle(LegacyPing packet) {
    getInitialPing()
        .thenCompose(ping -> server.getEventManager().fire(new ProxyPingEvent(inbound, ping)))
        .thenAcceptAsync(event -> {
          connection.closeWith(LegacyDisconnect.fromServerPing(event.getPing(),
              packet.getVersion()));
        }, connection.eventLoop());
    return true;
  }

  @Override
  public boolean handle(StatusPing packet) {
    connection.closeWith(packet);
    return true;
  }

  @Override
  public boolean handle(StatusRequest packet) {
    getInitialPing()
        .thenCompose(ping -> server.getEventManager().fire(new ProxyPingEvent(inbound, ping)))
        .thenAcceptAsync(
            (event) -> {
              StringBuilder json = new StringBuilder();
              VelocityServer.GSON.toJson(event.getPing(), json);
              connection.write(new StatusResponse(json));
            },
            connection.eventLoop());
    return true;
  }

  @Override
  public void handleUnknown(ByteBuf buf) {
    // what even is going on?
    connection.close(true);
  }
}
