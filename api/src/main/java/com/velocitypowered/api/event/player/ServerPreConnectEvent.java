/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder.Status;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This event is fired before the player connects to a server.
 */
public final class ServerPreConnectEvent implements
    ResultedEvent<ServerPreConnectEvent.ServerResult> {

  private final Player player;
  private final RegisteredServer originalServer;
  private ServerResult result;

  /**
   * Creates the ServerPreConnectEvent.
   * @param player the player who is connecting to a server
   * @param originalServer the server the player was trying to connect to
   */
  public ServerPreConnectEvent(Player player, RegisteredServer originalServer) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.originalServer = Preconditions.checkNotNull(originalServer, "originalServer");
    this.result = ServerResult.allowed(originalServer);
  }

  /**
   * Returns the player connecting to the server.
   * @return the player connecting to the server
   */
  public Player getPlayer() {
    return player;
  }

  @Override
  public ServerResult getResult() {
    return result;
  }

  @Override
  public void setResult(ServerResult result) {
    this.result = Preconditions.checkNotNull(result, "result");
  }

  /**
   * Returns the server that the player originally tried to connect to. To get the server the
   * player will connect to, see the {@link ServerResult} of this event. To get the server the
   * player is currently on when this event is fired, use {@link Player#getCurrentServer()}.
   * @return the server that the player originally tried to connect to
   */
  public RegisteredServer getOriginalServer() {
    return originalServer;
  }

  @Override
  public String toString() {
    return "ServerPreConnectEvent{"
        + "player=" + player
        + ", originalServer=" + originalServer
        + ", result=" + result
        + '}';
  }

  /**
   * Represents the result of the {@link ServerPreConnectEvent}.
   */
  public static class ServerResult implements ResultedEvent.Result {

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
     * is used, then {@link ConnectionRequestBuilder#connect()}'s result will have the status
     * {@link Status#CONNECTION_CANCELLED}.
     * @return a result to deny conneections
     */
    public static ServerResult denied() {
      return DENIED;
    }

    /**
     * Allows the player to connect to the specified server.
     * @param server the new server to connect to
     * @return a result to allow the player to connect to the specified server
     */
    public static ServerResult allowed(RegisteredServer server) {
      Preconditions.checkNotNull(server, "server");
      return new ServerResult(server);
    }
  }
}
