package com.velocitypowered.api.proxy;

import com.velocitypowered.api.server.ServerInfo;

/**
 * Represents a connection to a backend server from the proxy for a client.
 */
public interface ServerConnection {
    ServerInfo getServerInfo();

    Player getPlayer();
}
