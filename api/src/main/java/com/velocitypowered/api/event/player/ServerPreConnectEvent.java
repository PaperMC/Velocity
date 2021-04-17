/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.proxy.connection.Player;
import com.velocitypowered.api.proxy.player.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.player.ConnectionRequestBuilder.Status;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This event is fired before the player connects to a server.
 */
public interface ServerPreConnectEvent extends ResultedEvent<ServerPreConnectEvent.ServerResult> {

  /**
   * Returns the player connecting to the server.
   *
   * @return the player connecting to the server
   */
  Player getPlayer();

  @Override
  ServerResult getResult();

  @Override
  void setResult(ServerResult result);

  /**
   * Returns the server that the player originally tried to connect to. To get the server the player
   * will connect to, see the {@link ServerResult} of this event. To get the server the player is
   * currently on when this event is fired, use {@link Player#getCurrentServer()}.
   *
   * @return the server that the player originally tried to connect to
   */
  RegisteredServer getOriginalServer();

  /**
   * Represents the result of the {@link ServerPreConnectEvent}.
   */
  class ServerResult implements Result {

    private static final ServerResult DENIED = new ServerResult(null);

    private final @Nullable RegisteredServer server;

    private ServerResult(@Nullable RegisteredServer server) {
      this.server = server;
    }

    @Override
    public boolean isAllowed() {
      return server != null;
    }

    public Optional<RegisteredServer> getServer() {
      return Optional.ofNullable(server);
    }

    @Override
    public String toString() {
      if (server != null) {
        return "allowed: connect to " + server.getServerInfo().getName();
      }
      return "denied";
    }

    /**
     * Returns a result that will prevent players from connecting to another server. If this result
     * is used, then {@link ConnectionRequestBuilder#connect()}'s result will have the status {@link
     * Status#CONNECTION_CANCELLED}.
     *
     * @return a result to deny conneections
     */
    public static ServerResult denied() {
      return DENIED;
    }

    /**
     * Allows the player to connect to the specified server.
     *
     * @param server the new server to connect to
     * @return a result to allow the player to connect to the specified server
     */
    public static ServerResult allowed(RegisteredServer server) {
      Preconditions.checkNotNull(server, "server");
      return new ServerResult(server);
    }
  }
}
