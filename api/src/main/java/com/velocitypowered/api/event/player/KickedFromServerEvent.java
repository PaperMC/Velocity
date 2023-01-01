/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Fired when a player is kicked from a server. You may either allow Velocity to kick the player
 * (with an optional reason override) or redirect the player to a separate server. By default,
 * Velocity will notify the user (if they are already connected to a server) or disconnect them
 * (if they are not on a server and no other servers are available). Velocity will wait on this
 * event to finish firing before taking the specified action.
 */
@AwaitingEvent
public final class KickedFromServerEvent implements
    ResultedEvent<KickedFromServerEvent.ServerKickResult> {

  private final Player player;
  private final RegisteredServer server;
  private final net.kyori.adventure.text.@Nullable Component originalReason;
  private final boolean duringServerConnect;
  private ServerKickResult result;

  /**
   * Creates a {@code KickedFromServerEvent} instance.
   *
   * @param player the player affected
   * @param server the server the player disconnected from
   * @param originalReason the reason for being kicked, optional
   * @param duringServerConnect whether or not the player was kicked during the connection process
   * @param result the initial result
   */
  public KickedFromServerEvent(Player player, RegisteredServer server,
      net.kyori.adventure.text.@Nullable Component originalReason,
      boolean duringServerConnect, ServerKickResult result) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.server = Preconditions.checkNotNull(server, "server");
    this.originalReason = originalReason;
    this.duringServerConnect = duringServerConnect;
    this.result = Preconditions.checkNotNull(result, "result");
  }

  @Override
  public ServerKickResult getResult() {
    return result;
  }

  @Override
  public void setResult(@NonNull ServerKickResult result) {
    this.result = Preconditions.checkNotNull(result, "result");
  }

  public Player getPlayer() {
    return player;
  }

  public RegisteredServer getServer() {
    return server;
  }

  public Optional<net.kyori.adventure.text.Component> getServerKickReason() {
    return Optional.ofNullable(originalReason);
  }

  /**
   * Returns whether or not the player got kicked while connecting to another server.
   *
   * @return whether or not the player got kicked
   */
  public boolean kickedDuringServerConnect() {
    return duringServerConnect;
  }

  /**
   * Returns whether or not the player got kicked while logging in.
   *
   * @return whether or not the player got kicked
   * @deprecated {@link #kickedDuringServerConnect()} has a better name and reflects the actual
   *     result
   */
  @Deprecated
  public boolean kickedDuringLogin() {
    return duringServerConnect;
  }

  /**
   * Represents the base interface for {@link KickedFromServerEvent} results.
   */
  public interface ServerKickResult extends ResultedEvent.Result {

  }

  /**
   * Tells the proxy to disconnect the player with the specified reason.
   */
  public static final class DisconnectPlayer implements ServerKickResult {

    private final net.kyori.adventure.text.Component component;

    private DisconnectPlayer(net.kyori.adventure.text.Component component) {
      this.component = Preconditions.checkNotNull(component, "component");
    }

    @Override
    public boolean isAllowed() {
      return true;
    }

    public net.kyori.adventure.text.Component getReasonComponent() {
      return component;
    }

    /**
     * Creates a new {@link DisconnectPlayer} with the specified reason.
     *
     * @param reason the reason to use when disconnecting the player
     * @return the disconnect result
     */
    public static DisconnectPlayer create(net.kyori.adventure.text.Component reason) {
      return new DisconnectPlayer(reason);
    }
  }

  /**
   * Tells the proxy to redirect the player to another server.
   */
  public static final class RedirectPlayer implements ServerKickResult {

    private final net.kyori.adventure.text.Component message;
    private final RegisteredServer server;

    private RedirectPlayer(RegisteredServer server,
        net.kyori.adventure.text.@Nullable Component message) {
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

    public net.kyori.adventure.text.@Nullable Component getMessageComponent() {
      return message;
    }

    /**
     * Creates a new redirect result to forward the player to the specified {@code server}.
     * The specified {@code message} will be sent to the player after the redirection.
     * Use {@code Component.empty()} to skip sending any messages to the player.
     *
     * @param server the server to send the player to
     * @param message the message will be sent to the player after redirecting
     * @return the redirect result
     */
    public static RedirectPlayer create(RegisteredServer server,
        net.kyori.adventure.text.Component message) {
      return new RedirectPlayer(server, message);
    }

    /**
     * Creates a new redirect result to forward the player to the specified {@code server}.
     * The kick reason will be displayed to the player
     *
     * @param server the server to send the player to
     * @return the redirect result
     */
    public static ServerKickResult create(RegisteredServer server) {
      return new RedirectPlayer(server, null);
    }
  }

  /**
   * Notifies the player with the specified message but does nothing else. This is only a valid
   * result to use if the player was  trying to connect to a different server, otherwise it is
   * treated like a {@link DisconnectPlayer} result.
   */
  public static final class Notify implements ServerKickResult {

    private final net.kyori.adventure.text.Component message;

    private Notify(net.kyori.adventure.text.Component message) {
      this.message = Preconditions.checkNotNull(message, "message");
    }

    @Override
    public boolean isAllowed() {
      return false;
    }

    public net.kyori.adventure.text.Component getMessageComponent() {
      return message;
    }

    /**
     * Notifies the player with the specified message but does nothing else.
     *
     * @param message the server to send the player to
     * @return the redirect result
     */
    public static Notify create(net.kyori.adventure.text.Component message) {
      return new Notify(message);
    }
  }
}
