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
import java.util.Objects;
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
  Player player();

  /**
   * Returns the server that the player originally tried to connect to. To get the server the player
   * will connect to, see the {@link ServerResult} of this event. To get the server the player is
   * currently on when this event is fired, use {@link Player#connectedServer()}.
   *
   * @return the server that the player originally tried to connect to
   */
  RegisteredServer originalTarget();

  default void reject() {
    setResult(ServerResult.DENIED);
  }

  /**
   * Represents the result of the {@link ServerPreConnectEvent}.
   */
  class ServerResult implements Result {

    private static final ServerResult DENIED = new ServerResult(null);

    private final @Nullable RegisteredServer target;

    private ServerResult(@Nullable RegisteredServer target) {
      this.target = target;
    }

    @Override
    public boolean isAllowed() {
      return target != null;
    }

    public @Nullable RegisteredServer target() {
      return target;
    }

    @Override
    public String toString() {
      if (target != null) {
        return "allowed: connect to " + target.serverInfo().name();
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
     * @param target the new server to connect to
     * @return a result to allow the player to connect to the specified server
     */
    public static ServerResult allowed(RegisteredServer target) {
      Preconditions.checkNotNull(target, "server");
      return new ServerResult(target);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof ServerResult)) {
        return false;
      }
      ServerResult that = (ServerResult) o;
      return Objects.equals(target, that.target);
    }

    @Override
    public int hashCode() {
      return Objects.hash(target);
    }
  }
}
