package com.velocitypowered.proxy.server;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.proxy.VelocityServer;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServerMap {
    private final VelocityServer server;
    private final Map<String, RegisteredServer> servers = new HashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public ServerMap(VelocityServer server) {
        this.server = server;
    }

    public Optional<RegisteredServer> getServer(String name) {
        Preconditions.checkNotNull(name, "server");
        String lowerName = name.toLowerCase(Locale.US);
        lock.readLock().lock();
        try {
            return Optional.ofNullable(servers.get(lowerName));
        } finally {
            lock.readLock().unlock();
        }
    }

    public Collection<RegisteredServer> getAllServers() {
        lock.readLock().lock();
        try {
            return ImmutableList.copyOf(servers.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    public RegisteredServer register(ServerInfo serverInfo) {
        Preconditions.checkNotNull(serverInfo, "serverInfo");
        String lowerName = serverInfo.getName().toLowerCase(Locale.US);
        lock.writeLock().lock();
        try {
            VelocityRegisteredServer rs = new VelocityRegisteredServer(server, serverInfo);
            Preconditions.checkArgument(servers.putIfAbsent(lowerName, rs) == null, "Server with name %s already registered", serverInfo.getName());
            return rs;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void unregister(ServerInfo serverInfo) {
        Preconditions.checkNotNull(serverInfo, "serverInfo");
        String lowerName = serverInfo.getName().toLowerCase(Locale.US);
        lock.writeLock().lock();
        try {
            RegisteredServer rs = servers.get(lowerName);
            Preconditions.checkArgument(rs != null, "Server with name %s is not registered!", serverInfo.getName());
            Preconditions.checkArgument(rs.getServerInfo().equals(serverInfo), "Trying to remove server %s with differing information", serverInfo.getName());
            servers.remove(lowerName);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
