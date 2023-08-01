package com.velocitypowered.api.event.proxy;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.server.ServerInfo;

/**
 * @author 254n_m
 * @since 2023/08/01 12:54 AM
 * This file was created as a part of velocity
 * This event is fired by the proxy after a new backend server is registered
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
