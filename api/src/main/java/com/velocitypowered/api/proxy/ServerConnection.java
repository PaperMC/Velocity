package com.velocitypowered.api.proxy;

import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import com.velocitypowered.api.proxy.messages.ChannelMessageSource;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

/**
 * Represents a connection to a backend server from the proxy for a client.
 */
public interface ServerConnection extends ChannelMessageSource, ChannelMessageSink {

  /**
   * Returns the server that this connection is connected to.
   *
   * @return the server this connection is connected to
   */
  RegisteredServer getProxy();

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
}
