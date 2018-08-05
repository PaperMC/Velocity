package com.velocitypowered.proxy.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.server.ServerInfo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServerMap {
    private final Map<String, ServerInfo> servers = new HashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public Optional<ServerInfo> getServer(String server) {
        Preconditions.checkNotNull(server, "server");
        lock.readLock().lock();
        try {
            return Optional.ofNullable(servers.get(server.toLowerCase()));
        } finally {
            lock.readLock().unlock();
        }
    }

    public Collection<ServerInfo> getAllServers() {
        lock.readLock().lock();
        try {
            return ImmutableList.copyOf(servers.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    public void register(ServerInfo server) {
        Preconditions.checkNotNull(server, "server");
        lock.writeLock().lock();
        try {
            Preconditions.checkArgument(servers.putIfAbsent(server.getName(), server) == null, "Server with name %s already registered", server.getName());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void unregister(ServerInfo server) {
        Preconditions.checkNotNull(server, "server");
        lock.writeLock().lock();
        try {
            Preconditions.checkArgument(servers.remove(server.getName(), server), "Server with this name is not registered!");
        } finally {
            lock.writeLock().unlock();
        }
    }
}
