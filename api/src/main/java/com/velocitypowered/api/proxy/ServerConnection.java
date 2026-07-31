/*
 * Copyright (C) 2018-2022 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy;

import com.google.common.annotations.Beta;
import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import com.velocitypowered.api.proxy.messages.ChannelMessageSource;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import java.util.Optional;

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
   * Returns the server that the player associated with this connection was connected to before
   * switching to this connection.
   *
   * @return the server the player was connected to.
   */
  Optional<RegisteredServer> getPreviousServer();

  /**
   * Returns the server info for this connection.
   *
   * @return the server info for this connection
   */
  ServerInfo getServerInfo();

  /**
   * Returns whether the client notified this connection of having loaded the world.
   *
   * @return true if the client has loaded the world
   * @implNote This is purely client-dependent; see {@link com.velocitypowered.api.event.player.PlayerClientLoadedWorldEvent}.
   * @sinceMinecraft 1.21.4
   * @since 4.1.0
   */
  @Beta
  boolean isClientLoaded();

  /**
   * Returns the player that this connection is associated with.
   *
   * @return the player for this connection
   */
  Player getPlayer();
}
