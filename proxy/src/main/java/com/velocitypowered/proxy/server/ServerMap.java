package com.velocitypowered.proxy.server;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.proxy.VelocityProxy;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ServerMap {

  private final @Nullable VelocityProxy proxy;
  private final Map<String, RegisteredServer> servers = new ConcurrentHashMap<>();

  public ServerMap(@Nullable VelocityProxy proxy) {
    this.proxy = proxy;
  }

  /**
   * Returns the server associated with the given name.
   *
   * @param name the name to look up
   * @return the server, if it exists
   */
  public Optional<RegisteredServer> getServer(String name) {
    Preconditions.checkNotNull(name, "server");
    String lowerName = name.toLowerCase(Locale.US);
    return Optional.ofNullable(servers.get(lowerName));
  }

  public Collection<RegisteredServer> getAllServers() {
    return ImmutableList.copyOf(servers.values());
  }

  /**
   * Registers a server with the proxy.
   *
   * @param serverInfo the server to register
   * @return the registered server
   */
  public RegisteredServer register(ServerInfo serverInfo) {
    Preconditions.checkNotNull(serverInfo, "serverInfo");
    String lowerName = serverInfo.getName().toLowerCase(Locale.US);
    VelocityRegisteredServer rs = new VelocityRegisteredServer(proxy, serverInfo);

    RegisteredServer existing = servers.putIfAbsent(lowerName, rs);
    if (existing != null && !existing.getServerInfo().equals(serverInfo)) {
      throw new IllegalArgumentException(
          "Server with name " + serverInfo.getName() + " already registered");
    } else if (existing == null) {
      return rs;
    } else {
      return existing;
    }
  }

  /**
   * Unregisters the specified server from the proxy.
   *
   * @param serverInfo the server to unregister
   */
  public void unregister(ServerInfo serverInfo) {
    Preconditions.checkNotNull(serverInfo, "serverInfo");
    String lowerName = serverInfo.getName().toLowerCase(Locale.US);
    RegisteredServer rs = servers.get(lowerName);
    if (rs == null) {
      throw new IllegalArgumentException(
          "Server with name " + serverInfo.getName() + " is not registered!");
    }
    Preconditions.checkArgument(rs.getServerInfo().equals(serverInfo),
        "Trying to remove server %s with differing information", serverInfo.getName());
    Preconditions.checkState(servers.remove(lowerName, rs),
        "Server with name %s replaced whilst unregistering", serverInfo.getName());
  }
}
