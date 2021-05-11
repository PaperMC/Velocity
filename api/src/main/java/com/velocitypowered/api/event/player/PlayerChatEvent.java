/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.ResultedEvent.GenericResult;
import com.velocitypowered.api.proxy.connection.Player;

public interface PlayerChatEvent extends ResultedEvent<GenericResult> {

  /**
   * Returns the player sending the message.
   *
   * @return the player sending the message
   */
  Player player();

  /**
   * Returns the message the player originally sent.
   *
   * @return the message the player originally sent
   */
  String originalMessage();

  /**
   * Returns the message currently being sent, which may be modified by plugins.
   *
   * @return the message currently being sent
   */
  String currentMessage();

  /**
   * Sets a new message to send, if the message is allowed to be sent.
   *
   * @param currentMessage the message to send instead of the current (or original) message
   */
  void setCurrentMessage(String currentMessage);
}
