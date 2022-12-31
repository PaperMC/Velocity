/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.api.event.connection;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.player.ServerHandshake;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This event is fired after the {@link com.velocitypowered.api.event.player.ServerPreConnectEvent}
 * to enable plugins modifying the handshake sent to the server.
 *
 * <p>
 *   Velocity will wait for this event to finish firing before proceeding with the rest of the login
 *   process, but you should try to limit the work done in any event that fires during the login
 *   process.
 * </p>
 */
@AwaitingEvent
public final class ServerHandshakeEvent {
  private final Player player;
  private final RegisteredServer server;
  private final ProtocolVersion protocolVersion;
  private final int nextStatus;
  private final ServerHandshake originalHandshake;
  private @Nullable ServerHandshake handshake;

  /**
   * Creates a new instance.
   *
   * @param player            the player connecting to the server
   * @param server            the server the player is connecting to
   * @param protocolVersion   the protocol version to send to the server
   * @param nextStatus        the next status to send to the server
   * @param originalHandshake the original {@link ServerHandshake} to send to the server
   */
  public ServerHandshakeEvent(Player player, RegisteredServer server,
      ProtocolVersion protocolVersion, int nextStatus, ServerHandshake originalHandshake) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.server = Preconditions.checkNotNull(server, "server");
    this.protocolVersion = Preconditions.checkNotNull(protocolVersion, "protocolVersion");
    this.nextStatus = nextStatus;
    this.originalHandshake = Preconditions.checkNotNull(originalHandshake);
  }

  /**
   * Returns the player connecting to the server.
   *
   * @return the player connecting to the server
   */
  public Player getPlayer() {
    return player;
  }

  /**
   * Returns the server the player is connecting to.
   *
   * @return the server the player is connecting to
   */
  public RegisteredServer getServer() {
    return server;
  }

  /**
   * Returns the protocol version to send to the server.
   *
   * @return the protocol version
   */
  public ProtocolVersion getProtocolVersion() {
    return protocolVersion;
  }

  /**
   * Returns the next status to send to the server.
   *
   * @return the next status
   */
  public int getNextStatus() {
    return nextStatus;
  }

  public ServerHandshake getOriginalHandshake() {
    return originalHandshake;
  }

  /**
   * Returns the handshake to send to the server.
   *
   * @return the {@link ServerHandshakeEvent}
   */
  public ServerHandshake getHandshake() {
    return handshake == null ? originalHandshake : handshake;
  }

  /**
   * Sets the handshake to send to the server.
   *
   * @param handshake the handshake to send to the server, {@code null} uses the original handshake
   */
  public void setHandshake(@Nullable ServerHandshake handshake) {
    this.handshake = handshake;
  }

  @Override
  public String toString() {
    return "ServerHandshakeEvent{"
        + "handshake=" + handshake
        + '}';
  }
}
