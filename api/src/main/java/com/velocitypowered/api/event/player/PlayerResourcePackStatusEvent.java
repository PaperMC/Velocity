package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;

public class PlayerResourcePackStatusEvent {
  private final Player player;
  private final Status result;

  public PlayerResourcePackStatusEvent(Player player, Status result) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.result = Preconditions.checkNotNull(result, "result");
  }

  public Player getPlayer() {
    return player;
  }

  public Status getResult() {
    return result;
  }

  @Override
  public String toString() {
    return "PlayerResourcePackStatusEvent{"
        + "player=" + player
        + ", result=" + result
        + '}';
  }

  public enum Status {
    /**
     * The resource pack was applied successfully.
     */
    SUCCESSFUL,
    /**
     * The player declined to download the resource pack.
     */
    DECLINED,
    /**
     * The player could not download the resource pack.
     */
    FAILED_DOWNLOAD,
    /**
     * The player has accepted the resource pack and is now downloading it.
     */
    ACCEPTED
  }
}
