package com.velocitypowered.api.event.proxy;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.server.ServerInfo;

/**
 * This event is fired by the proxy after a new backend server is registered
 * "registered" in this context means new backend server was added to the proxy dynamically at runtime.
 *  @see com.velocitypowered.api.proxy.ProxyServer#registerServer(ServerInfo)
 */
public class ServerRegisteredEvent {
    private final ServerInfo serverInfo;

    public ServerRegisteredEvent(ServerInfo serverInfo) {
        this.serverInfo = Preconditions.checkNotNull(serverInfo, "serverInfo");
    }

    public ServerInfo getServerInfo() {
        return serverInfo;
    }
    @Override
    public String toString() {
        return "ServerRegisteredEvent{"
                + "serverInfo=" + serverInfo
                + '}';
    }
}
