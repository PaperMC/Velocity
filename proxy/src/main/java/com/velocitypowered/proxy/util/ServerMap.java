package com.velocitypowered.proxy.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.server.ServerInfo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ServerMap {
    private final Map<String, ServerInfo> servers = new HashMap<>();

    public Optional<ServerInfo> getServer(String name) {
        Preconditions.checkNotNull(name, "name");
        return Optional.ofNullable(servers.get(name.toLowerCase()));
    }

    public Collection<ServerInfo> getAllServers() {
        return ImmutableList.copyOf(servers.values());
    }

    public void register(ServerInfo info) {
        Preconditions.checkNotNull(info, "info");
        servers.put(info.getName(), info);
    }
}
