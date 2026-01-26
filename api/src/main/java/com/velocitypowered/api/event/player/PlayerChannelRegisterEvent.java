/*
 * Copyright (C) 2021-2022 Velocity Contributors
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
 * register channel. Velocity will not wait on this event to finish firing.
 */
public final class PlayerChannelRegisterEvent {

  private final Player player;
  private final List<ChannelIdentifier> channels;

  /**
   * Constructs a new PlayerChannelRegisterEvent.
   *
   * @param player the player who sent the plugin message
   * @param channels the list of channels the player is registering
   */
  public PlayerChannelRegisterEvent(Player player, List<ChannelIdentifier> channels) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.channels = Preconditions.checkNotNull(channels, "channels");
  }

  /**
   * Gets the player who sent the plugin message to register channels.
   *
   * @return the player involved in this event
   */
  public Player getPlayer() {
    return player;
  }

  /**
   * Gets the list of {@link ChannelIdentifier}s that the player registered.
   *
   * @return the list of registered channels
   */
  public List<ChannelIdentifier> getChannels() {
    return channels;
  }

  @Override
  public String toString() {
    return "PlayerChannelRegisterEvent{"
            + "player=" + player
            + ", channels=" + channels
            + '}';
  }
}
