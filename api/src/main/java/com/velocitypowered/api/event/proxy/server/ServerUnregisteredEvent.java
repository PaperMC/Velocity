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
 * This event is fired by the proxy after a backend server is unregistered from the server map.
 * Currently, it may occur when a server is unregistered dynamically at runtime
 * or when a server is replaced due to configuration reload.
 *
 * @see com.velocitypowered.api.proxy.ProxyServer#unregisterServer(ServerInfo)
 *
 * @param unregisteredServer A {@link RegisteredServer} that has been unregistered.
 * @since 3.3.0
 */
public record ServerUnregisteredEvent(@NotNull RegisteredServer unregisteredServer) {

  /**
   * Constructs a {@link ServerUnregisteredEvent}.
   *
   * @param unregisteredServer the server that was unregistered
   */
  public ServerUnregisteredEvent {
    Preconditions.checkNotNull(unregisteredServer, "unregisteredServer");
  }
}
