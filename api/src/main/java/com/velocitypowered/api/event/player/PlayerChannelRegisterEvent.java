package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;

import java.util.List;

/**
 * This event is fired when a client ({@link Player}) sends a plugin message through the
 * register channel.
 */
public final class PlayerChannelRegisterEvent {

  private final Player player;
  private final List<ChannelIdentifier> channels;

  public PlayerChannelRegisterEvent(Player player, List<ChannelIdentifier> channels) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.channels = Preconditions.checkNotNull(channels, "channels");
  }

  public Player getPlayer() {
    return player;
  }

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
