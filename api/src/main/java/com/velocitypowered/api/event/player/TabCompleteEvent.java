package com.velocitypowered.api.event.player;

import static com.google.common.base.Preconditions.checkNotNull;

import com.velocitypowered.api.proxy.Player;
import java.util.ArrayList;
import java.util.List;

/**
 * This event is fired after a tab complete response is sent by the remote server, for clients on
 * 1.12.2 and below. You have the opportunity to modify the response sent to the remote player.
 */
public class TabCompleteEvent {
  private final Player player;
  private final String partialMessage;
  private final List<String> suggestions;

  public TabCompleteEvent(Player player, String partialMessage, List<String> suggestions) {
    this.player = checkNotNull(player, "player");
    this.partialMessage = checkNotNull(partialMessage, "partialMessage");
    this.suggestions = new ArrayList<>(checkNotNull(suggestions, "suggestions"));
  }

  /**
   * Returns the player requesting the tab completion.
   * @return the requesting player
   */
  public Player getPlayer() {
    return player;
  }

  /**
   * Returns the message being partially completed.
   * @return
   */
  public String getPartialMessage() {
    return partialMessage;
  }

  public List<String> getSuggestions() {
    return suggestions;
  }

  @Override
  public String toString() {
    return "TabCompleteEvent{"
        + "player=" + player
        + ", partialMessage='" + partialMessage + '\''
        + ", suggestions=" + suggestions
        + '}';
  }
}
