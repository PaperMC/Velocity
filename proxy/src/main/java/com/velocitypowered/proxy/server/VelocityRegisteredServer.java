package com.velocitypowered.proxy.server;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class VelocityRegisteredServer implements RegisteredServer {
    private final VelocityServer server;
    private final ServerInfo serverInfo;
    private final Set<ConnectedPlayer> players = new HashSet<>();
    private final ReadWriteLock playersLock = new ReentrantReadWriteLock();

    public VelocityRegisteredServer(VelocityServer server, ServerInfo serverInfo) {
        this.server = server;
        this.serverInfo = serverInfo;
    }

    @Override
    public ServerInfo getServerInfo() {
        return serverInfo;
    }

    @Override
    public Collection<Player> getPlayersConnected() {
        playersLock.readLock().lock();
        try {
            return ImmutableList.copyOf(players);
        } finally {
            playersLock.readLock().unlock();
        }
    }

    @Override
    public CompletableFuture<ServerPing> ping() {
        CompletableFuture<ServerPing> p = new CompletableFuture<>();
        p.completeExceptionally(new UnsupportedOperationException("Not currently implemented."));
        return p;
    }

    public void addPlayer(ConnectedPlayer player) {
        playersLock.writeLock().lock();
        try {
            players.add(player);
        } finally {
            playersLock.writeLock().unlock();
        }
    }

    public void removePlayer(ConnectedPlayer player) {
        playersLock.writeLock().lock();
        try {
            players.remove(player);
        } finally {
            playersLock.writeLock().unlock();
        }
    }

    @Override
    public boolean sendPluginMessage(ChannelIdentifier identifier, byte[] data) {
        ServerConnection backendConnection = null;
        playersLock.readLock().lock();
        try {
            for (ConnectedPlayer player : players) {
                if (player.getConnectedServer() != null && player.getConnectedServer().getServerInfo().equals(serverInfo)) {
                    backendConnection = player.getConnectedServer();
                    break;
                }
            }

            if (backendConnection == null) {
                return false;
            }
        } finally {
            playersLock.readLock().unlock();
        }

        return backendConnection.sendPluginMessage(identifier, data);
    }

    @Override
    public String toString() {
        return "registered server: " + serverInfo;
    }
}
