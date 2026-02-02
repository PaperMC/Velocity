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
import org.jetbrains.annotations.NotNull;

/**
 * This event is fired by the proxy after a backend server is registered to the server map.
 * Currently, it may occur when a server is registered dynamically at runtime or when a server is
 * replaced due to configuration reload.
 *
 * @see com.velocitypowered.api.proxy.ProxyServer#registerServer(ServerInfo)
 *
 * @param registeredServer A {@link RegisteredServer} that has been registered.
 * @since 3.3.0
 */
public record ServerRegisteredEvent(@NotNull RegisteredServer registeredServer) {

  /**
   * Constructs a new {@link ServerRegisteredEvent}.
   *
   * @param registeredServer the server that was registered
   */
  public ServerRegisteredEvent {
    Preconditions.checkNotNull(registeredServer, "registeredServer");
  }
}
