/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy;

import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import com.velocitypowered.api.proxy.messages.ChannelMessageSource;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/**
 * Represents a connection to a backend server from the proxy for a client.
 */
public interface ServerConnection extends ChannelMessageSource, ChannelMessageSink {

  /**
   * Returns the server that this connection is connected to.
   *
   * @return the server this connection is connected to
   */
  RegisteredServer getServer();

  /**
   * Returns the server info for this connection.
   *
   * @return the server info for this connection
   */
  ServerInfo getServerInfo();

  /**
   * Returns the player that this connection is associated with.
   *
   * @return the player for this connection
   */
  Player getPlayer();

  /**
   * Returns the world the current connection's associated {@link Player} is playing on.
   *
   * @return an {@link Optional} the name of the world that the player is playing on, which may be
   *     empty.
   */
  Optional<Key> getCurrentWorldName();
}
