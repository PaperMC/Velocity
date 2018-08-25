package com.velocitypowered.proxy.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.server.ServerInfo;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServerMap {
    private final Map<String, ServerInfo> servers = new HashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public Optional<ServerInfo> getServer(String server) {
        Preconditions.checkNotNull(server, "server");
        String lowerName = server.toLowerCase(Locale.US);
        lock.readLock().lock();
        try {
            return Optional.ofNullable(servers.get(lowerName));
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
        String lowerName = server.getName().toLowerCase(Locale.US);
        lock.writeLock().lock();
        try {
            Preconditions.checkArgument(servers.putIfAbsent(lowerName, server) == null, "Server with name %s already registered", server.getName());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void unregister(ServerInfo server) {
        Preconditions.checkNotNull(server, "server");
        String lowerName = server.getName().toLowerCase(Locale.US);
        lock.writeLock().lock();
        try {
            Preconditions.checkArgument(servers.remove(lowerName, server), "Server with this name is not registered!");
        } finally {
            lock.writeLock().unlock();
        }
    }
}
