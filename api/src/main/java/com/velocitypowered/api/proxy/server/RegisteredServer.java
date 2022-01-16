/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.server;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.audience.Audience;

/**
 * Represents a server that has been registered with the proxy.
 *
 * <p>This is an immutable and stateless view over a server's information. To get the server's current state
 * (such as players connected), use {@link RegisteredServer#getState()}.
 * </p>
 */
public interface RegisteredServer extends ChannelMessageSink, Audience {

  /**
   * Returns the {@link ServerInfo} for this server.
   *
   * @return the server info
   */
  ServerInfo getServerInfo();

  /**
   * Returns a list of all the players currently connected to this server on this proxy.
   *
   * @return the players on this proxy
   * @deprecated use {@link ServerState#getPlayersConnected()} instead
   */
  @Deprecated
  default Collection<Player> getPlayersConnected() {
    return getState().getPlayersConnected();
  }

  /**
   * Attempts to ping the remote server and return the server list ping result.
   *
   * @return the server ping result from the server
   */
  CompletableFuture<ServerPing> ping();

  /**
   * Returns the current state of this server on the proxy.
   *
   * @return the server state
   */
  ServerState getState();

  /**
   * Sends a plugin message to all the players connected this server.
   *
   * @param identifier the channel identifier to send the message on
   * @param data the data to send
   * @return whether or not the message could be sent
   * @deprecated use {@link ServerState#sendPluginMessage(ChannelIdentifier, byte[])} instead
   */
  @Override
  @Deprecated
  default boolean sendPluginMessage(ChannelIdentifier identifier, byte[] data) {
    return getState().sendPluginMessage(identifier, data);
  }
}
