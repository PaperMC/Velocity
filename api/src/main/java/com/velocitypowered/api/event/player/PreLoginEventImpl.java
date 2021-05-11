/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.connection.InboundConnection;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * This event is fired when a player has initiated a connection with the proxy but before the proxy
 * authenticates the player with Mojang or before the player's proxy connection is fully established
 * (for offline mode).
 */
public final class PreLoginEventImpl implements PreLoginEvent {

  private final InboundConnection connection;
  private final String username;
  private ComponentResult result;
  private boolean onlineMode;

  /**
   * Creates a new instance.
   * @param connection the connection logging into the proxy
   * @param username the player's username
   * @param onlineMode whether or not the connection is online mode
   */
  public PreLoginEventImpl(InboundConnection connection, String username, boolean onlineMode) {
    this.connection = Preconditions.checkNotNull(connection, "connection");
    this.username = Preconditions.checkNotNull(username, "username");
    this.onlineMode = onlineMode;
    this.result = ComponentResult.allowed();
  }

  @Override
  public InboundConnection connection() {
    return connection;
  }

  @Override
  public String username() {
    return username;
  }

  @Override
  public boolean onlineMode() {
    return this.onlineMode;
  }

  @Override
  public void setOnlineMode(boolean onlineMode) {
    this.onlineMode = onlineMode;
  }

  @Override
  public ComponentResult result() {
    return result;
  }

  @Override
  public void setResult(@NonNull ComponentResult result) {
    this.result = Preconditions.checkNotNull(result, "result");
  }

  @Override
  public String toString() {
    return "PreLoginEvent{"
        + "connection=" + connection
        + ", username='" + username + '\''
        + ", result=" + result
        + '}';
  }

}
