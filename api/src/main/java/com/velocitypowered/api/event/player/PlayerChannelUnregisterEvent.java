/*
 * Copyright (C) 2025 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import java.util.List;

/**
 * This event is fired when a client ({@link Player}) sends a plugin message through the
 * unregister channel. Velocity will not wait on this event to finish firing.
 */
public final class PlayerChannelUnregisterEvent {

  private final Player player;
  private final List<ChannelIdentifier> channels;

  /**
   * Constructs a new {@link PlayerChannelUnregisterEvent}.
   *
   * @param player the player that sent the unregister message
   * @param channels the list of {@link ChannelIdentifier}s being unregistered
   * @throws NullPointerException if {@code player} or {@code channels} is {@code null}
   */
  public PlayerChannelUnregisterEvent(Player player, List<ChannelIdentifier> channels) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.channels = Preconditions.checkNotNull(channels, "channels");
  }

  /**
   * Gets the {@link Player} who sent the unregister message.
   *
   * @return the player involved in this event
   */
  public Player getPlayer() {
    return player;
  }

  /**
   * Gets the list of {@link ChannelIdentifier}s that the player has unregistered.
   *
   * <p>These identifiers correspond to the plugin message channels that the client has
   * indicated it will no longer use.</p>
   *
   * @return the list of unregistered channels
   */
  public List<ChannelIdentifier> getChannels() {
    return channels;
  }

  @Override
  public String toString() {
    return "PlayerChannelUnregisterEvent{"
            + "player=" + player
            + ", channels=" + channels
            + '}';
  }
}
