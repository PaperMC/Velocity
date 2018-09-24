package com.velocitypowered.api.proxy.server;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.ChannelMessageSink;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a server that has been registered with the proxy.
 */
public interface RegisteredServer extends ChannelMessageSink {
    /**
     * Returns the {@link ServerInfo} for this server.
     * @return the server info
     */
    ServerInfo getServerInfo();

    /**
     * Returns a list of all the players currently connected to this server on this proxy.
     * @return the players on this proxy
     */
    Collection<Player> getPlayersConnected();

    /**
     * Attempts to ping the remote server and return the server list ping result.
     * @return the server ping result from the server
     */
    CompletableFuture<ServerPing> ping();
}
