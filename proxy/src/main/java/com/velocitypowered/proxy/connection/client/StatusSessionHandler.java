package com.velocitypowered.proxy.connection.client;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.ModInfo;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.packet.LegacyDisconnect;
import com.velocitypowered.proxy.protocol.packet.LegacyPing;
import com.velocitypowered.proxy.protocol.packet.StatusPing;
import com.velocitypowered.proxy.protocol.packet.StatusRequest;
import com.velocitypowered.proxy.protocol.packet.StatusResponse;
import io.netty.buffer.ByteBuf;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

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

  private ServerPing createInitialPing() {
    VelocityConfiguration configuration = server.getConfiguration();
    ProtocolVersion shownVersion = ProtocolVersion.isSupported(connection.getProtocolVersion())
        ? connection.getProtocolVersion() : ProtocolVersion.MAXIMUM_VERSION;
    return new ServerPing(
        new ServerPing.Version(shownVersion.getProtocol(),
            "Velocity " + ProtocolVersion.SUPPORTED_VERSION_STRING),
        new ServerPing.Players(server.getPlayerCount(), configuration.getShowMaxPlayers(),
            ImmutableList.of()),
        configuration.getMotdComponent(),
        configuration.getFavicon().orElse(null),
        getModInfo(configuration.isAnnounceForge())
    );
  }

  private ModInfo getModInfo(boolean isAnnounceForge) {
    for (String serverName : server.getConfiguration().getAttemptModInfoOrder()) {
      Optional<RegisteredServer> registeredServer = server.getServer(serverName);
      if (!registeredServer.isPresent()) {
        continue;
      }
      try {
        ServerPing serverPing = registeredServer.get().ping().get();
        if (serverPing == null) {
          continue;
        }
        Optional<ModInfo> modInfo = serverPing.getModinfo();
        if (!modInfo.isPresent()) {
          continue;
        }
        return modInfo.get();
      } catch (InterruptedException | ExecutionException ignored) {
      }
    }
    return isAnnounceForge ? ModInfo.DEFAULT : null;
  }

  @Override
  public boolean handle(LegacyPing packet) {
    ServerPing initialPing = createInitialPing();
    ProxyPingEvent event = new ProxyPingEvent(inboundWrapper, initialPing);
    server.getEventManager().fire(event)
        .thenRunAsync(() -> {
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
    ServerPing initialPing = createInitialPing();
    ProxyPingEvent event = new ProxyPingEvent(inboundWrapper, initialPing);
    server.getEventManager().fire(event)
        .thenRunAsync(
            () -> {
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
