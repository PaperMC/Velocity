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
  private final String originalMessage;
  private String currentMessage;
  private GenericResult result;

  /**
   * Constructs a PlayerChatEvent.
   * @param player the player sending the message
   * @param message the message being sent
   */
  public PlayerChatEventImpl(Player player, String message) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.originalMessage = Preconditions.checkNotNull(message, "message");
    this.result = GenericResult.allowed();
    this.currentMessage = message;
  }

  @Override
  public Player player() {
    return player;
  }

  @Override
  public String originalMessage() {
    return originalMessage;
  }

  @Override
  public String currentMessage() {
    return currentMessage;
  }

  @Override
  public void setCurrentMessage(String currentMessage) {
    this.currentMessage = Preconditions.checkNotNull(currentMessage, "currentMessage");
  }

  @Override
  public GenericResult result() {
    return result;
  }

  @Override
  public void setResult(GenericResult result) {
    this.result = Preconditions.checkNotNull(result, "result");
  }

  @Override
  public String toString() {
    return "PlayerChatEvent{"
        + "player=" + player
        + ", message=" + originalMessage
        + ", result=" + result
        + '}';
  }

}
