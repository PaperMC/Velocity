package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;

import java.util.List;

/**
 * This event is fired when a client ({@link Player}) sends a plugin message through the
 * register channel.
 */
public final class PlayerPluginMessageRegisterEvent {

  private final Player player;
  private final List<String> channels;

  public PlayerPluginMessageRegisterEvent(Player player, List<String> channels) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.channels = Preconditions.checkNotNull(channels, "channels");
  }

  public Player getPlayer() {
    return player;
  }

  public List<String> getChannels() {
    return channels;
  }

  @Override
  public String toString() {
    return "PlayerPluginMessageRegisterEvent{"
            + "player=" + player
            + ", channels=" + channels
            + '}';
  }
}
