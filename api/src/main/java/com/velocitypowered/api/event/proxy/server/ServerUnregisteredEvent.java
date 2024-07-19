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
 * This event is fired by the proxy after a backend server is unregistered from the server map.
 * Currently, it may occur when a server is unregistered dynamically at runtime
 * or when a server is replaced due to configuration reload.
 *
 *  @see com.velocitypowered.api.proxy.ProxyServer#unregisterServer(ServerInfo)
 */
public class ServerUnregisteredEvent {

  private final RegisteredServer unregisteredServer;

  public ServerUnregisteredEvent(RegisteredServer unregisteredServer) {
    this.unregisteredServer = Preconditions.checkNotNull(unregisteredServer, "unregisteredServer");
  }

  public RegisteredServer getUnregisteredServer() {
    return unregisteredServer;
  }

  @Override
  public String toString() {
    return "ServerUnRegisteredEvent{"
         + "unregisteredServer=" + unregisteredServer
         + '}';
  }
}
