package com.velocitypowered.proxy.server;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.proxy.VelocityServer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerMap {
    private final VelocityServer server;
    private final Map<String, RegisteredServer> servers = new ConcurrentHashMap<>();

    public ServerMap(VelocityServer server) {
        this.server = server;
    }

    public Optional<RegisteredServer> getServer(String name) {
        Preconditions.checkNotNull(name, "server");
        String lowerName = name.toLowerCase(Locale.US);
        return Optional.ofNullable(servers.get(lowerName));
    }

    public Collection<RegisteredServer> getAllServers() {
        return ImmutableList.copyOf(servers.values());
    }

    public RegisteredServer register(ServerInfo serverInfo) {
        Preconditions.checkNotNull(serverInfo, "serverInfo");
        String lowerName = serverInfo.getName().toLowerCase(Locale.US);
        VelocityRegisteredServer rs = new VelocityRegisteredServer(server, serverInfo);
        Preconditions.checkArgument(servers.putIfAbsent(lowerName, rs) == null, "Server with name %s already registered", serverInfo.getName());
        return rs;
    }

    public void unregister(ServerInfo serverInfo) {
        Preconditions.checkNotNull(serverInfo, "serverInfo");
        String lowerName = serverInfo.getName().toLowerCase(Locale.US);
        RegisteredServer rs = servers.get(lowerName);
        Preconditions.checkArgument(rs != null, "Server with name %s is not registered!", serverInfo.getName());
        Preconditions.checkArgument(rs.getServerInfo().equals(serverInfo), "Trying to remove server %s with differing information", serverInfo.getName());
        Preconditions.checkState(servers.remove(lowerName, rs), "Server with name %s replaced whilst unregistering", serverInfo.getName());
    }
}
