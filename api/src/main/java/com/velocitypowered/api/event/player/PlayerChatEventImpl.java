/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.connection.Player;

/**
 * This event is fired when a player types in a chat message.
 */
public final class PlayerChatEventImpl implements PlayerChatEvent {

  private final Player player;
  private final String message;
  private ChatResult result;

  /**
   * Constructs a PlayerChatEvent.
   * @param player the player sending the message
   * @param message the message being sent
   */
  public PlayerChatEventImpl(Player player, String message) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.message = Preconditions.checkNotNull(message, "message");
    this.result = ChatResult.allowed();
  }

  @Override
  public Player getPlayer() {
    return player;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public ChatResult getResult() {
    return result;
  }

  @Override
  public void setResult(ChatResult result) {
    this.result = Preconditions.checkNotNull(result, "result");
  }

  @Override
  public String toString() {
    return "PlayerChatEvent{"
        + "player=" + player
        + ", message=" + message
        + ", result=" + result
        + '}';
  }

}
