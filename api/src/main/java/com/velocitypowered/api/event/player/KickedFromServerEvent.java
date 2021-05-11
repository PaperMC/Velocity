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
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Fired when a player is kicked from a server. You may either allow Velocity to kick the player
 * (with an optional reason override) or redirect the player to a separate server. By default,
 * Velocity will notify the user (if they are already connected to a server) or disconnect them
 * (if they are not on a server and no other servers are available).
 */
public interface KickedFromServerEvent extends
    ResultedEvent<KickedFromServerEvent.ServerKickResult> {

  Player player();

  RegisteredServer server();

  /**
   * Gets the reason the server kicked the player from the server.
   *
   * @return the server kicked the player from the server
   */
  Optional<Component> serverKickReason();

  /**
   * Returns whether or not the player got kicked while connecting to another server.
   *
   * @return whether or not the player got kicked
   */
  boolean kickedDuringServerConnect();

  /**
   * Handles the event by disconnecting the player with the specified {@code reason}.
   *
   * @param reason the reason for disconnecting the player
   */
  default void handleByDisconnecting(Component reason) {
    setResult(DisconnectPlayer.create(reason));
  }

  /**
   * Handles the event by falling back on the specified server.
   *
   * @param server the server to fall back to
   */
  default void handleByConnectingToServer(RegisteredServer server) {
    setResult(RedirectPlayer.create(server));
  }

  /**
   * Handles the event by giving the player the specified {@code reason}.
   *
   * @param reason the reason for being kicked
   */
  default void handleByNotifying(Component reason) {
    setResult(Notify.create(reason));
  }

  /**
   * Represents the base interface for {@link KickedFromServerEvent} results.
   */
  public interface ServerKickResult extends Result {

  }

  /**
   * Tells the proxy to disconnect the player with the specified reason.
   */
  final class DisconnectPlayer implements ServerKickResult {

    private final Component message;

    private DisconnectPlayer(Component message) {
      this.message = Preconditions.checkNotNull(message, "message");
    }

    @Override
    public boolean isAllowed() {
      return true;
    }

    public Component message() {
      return message;
    }

    /**
     * Creates a new {@link DisconnectPlayer} with the specified reason.
     *
     * @param reason the reason to use when disconnecting the player
     * @return the disconnect result
     */
    public static DisconnectPlayer create(Component reason) {
      return new DisconnectPlayer(reason);
    }
  }

  /**
   * Tells the proxy to redirect the player to another server. No messages will be sent from the
   * proxy when this result is used.
   */
  final class RedirectPlayer implements ServerKickResult {

    private final Component message;
    private final RegisteredServer server;

    private RedirectPlayer(RegisteredServer server,
        @Nullable Component message) {
      this.server = Preconditions.checkNotNull(server, "server");
      this.message = message;
    }

    @Override
    public boolean isAllowed() {
      return false;
    }

    public RegisteredServer getServer() {
      return server;
    }

    public Component message() {
      return message;
    }

    /**
     * Creates a new redirect result to forward the player to the specified {@code server}.
     *
     * @param server the server to send the player to
     * @return the redirect result
     */
    public static RedirectPlayer create(RegisteredServer server,
        Component message) {
      return new RedirectPlayer(server, message);
    }

    public static ServerKickResult create(RegisteredServer server) {
      return new RedirectPlayer(server, null);
    }
  }

  /**
   * Notifies the player with the specified message but does nothing else. This is only a valid
   * result to use if the player was  trying to connect to a different server, otherwise it is
   * treated like a {@link DisconnectPlayer} result.
   */
  final class Notify implements ServerKickResult {

    private final Component message;

    private Notify(Component message) {
      this.message = Preconditions.checkNotNull(message, "message");
    }

    @Override
    public boolean isAllowed() {
      return false;
    }

    public Component message() {
      return message;
    }

    /**
     * Notifies the player with the specified message but does nothing else.
     *
     * @param message the server to send the player to
     * @return the redirect result
     */
    public static Notify create(Component message) {
      return new Notify(message);
    }
  }
}
