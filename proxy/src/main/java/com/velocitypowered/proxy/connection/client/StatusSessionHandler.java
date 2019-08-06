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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class StatusSessionHandler implements MinecraftSessionHandler {

  private final VelocityServer server;
  private final MinecraftConnection connection;
  private final InboundConnection inboundWrapper;

  StatusSessionHandler(VelocityServer server, MinecraftConnection connection,
      InboundConnection inboundWrapper) {
    this.server = server;
    this.connection = connection;
    this.inboundWrapper = inboundWrapper;
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

  private CompletableFuture<ServerPing> createInitialPing() {
    VelocityConfiguration configuration = server.getConfiguration();
    ProtocolVersion shownVersion = ProtocolVersion.isSupported(connection.getProtocolVersion())
        ? connection.getProtocolVersion() : ProtocolVersion.MAXIMUM_VERSION;

    PingPassthroughMode passthrough = configuration.getPingPassthrough();
    if (passthrough == PingPassthroughMode.DISABLED) {
      return CompletableFuture.completedFuture(constructLocalPing(shownVersion));
    } else {
      return attemptPingPassthrough(configuration.getPingPassthrough(),
          configuration.getAttemptConnectionOrder(), shownVersion);
    }
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
          return responses.stream()
              .filter(ping -> ping != fallback)
              .findFirst()
              .orElse(fallback);
        });
      case MODS:
        return pingResponses.thenApply(responses -> {
          // Find the first non-fallback that contains a non-empty mod list
          Optional<ModInfo> modInfo = responses.stream()
              .filter(ping -> ping != fallback)
              .map(ServerPing::getModinfo)
              .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
              .findFirst();
          return modInfo.map(mi -> fallback.asBuilder().mods(mi).build()).orElse(fallback);
        });
      default:
        // Not possible, but covered for completeness.
        return CompletableFuture.completedFuture(fallback);
    }
  }

  @Override
  public boolean handle(LegacyPing packet) {
    createInitialPing()
        .thenCompose(ping -> server.getEventManager().fire(new ProxyPingEvent(inboundWrapper,
            ping)))
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
    createInitialPing()
        .thenCompose(ping -> server.getEventManager().fire(new ProxyPingEvent(inboundWrapper,
            ping)))
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
    connection.close();
  }
}
