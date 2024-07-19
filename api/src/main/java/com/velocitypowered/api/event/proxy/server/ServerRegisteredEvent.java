/*
 * Copyright (C) 2024 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.proxy.server;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

/**
 * This event is fired by the proxy after a backend server is registered to the server map.
 * Currently, it may occur when a server is registered dynamically at runtime or when a server is
 * replaced due to configuration reload.
 *
 *  @see com.velocitypowered.api.proxy.ProxyServer#registerServer(ServerInfo)
 */
public final class ServerRegisteredEvent {

  private final RegisteredServer registeredServer;

  public ServerRegisteredEvent(RegisteredServer registeredServer) {
    this.registeredServer = Preconditions.checkNotNull(registeredServer, "registeredServer");
  }

  public RegisteredServer getRegisteredServer() {
    return registeredServer;
  }

  @Override
  public String toString() {
    return "ServerRegisteredEvent{"
         + "registeredServer=" + registeredServer
         + '}';
  }
}
