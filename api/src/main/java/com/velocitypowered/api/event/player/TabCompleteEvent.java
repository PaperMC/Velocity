/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.velocitypowered.api.event.Event;
import com.velocitypowered.api.proxy.connection.Player;
import java.util.List;

/**
 * This event is fired after a tab complete response is sent by the remote server, for clients on
 * 1.12.2 and below. You have the opportunity to modify the response sent to the remote player.
 */
public interface TabCompleteEvent extends Event {

  /**
   * Returns the player requesting the tab completion.
   *
   * @return the requesting player
   */
  Player player();

  /**
   * Returns the message being partially completed.
   *
   * @return the partial message
   */
  String partialMessage();

  /**
   * Returns all the suggestions provided to the user, as a mutable list.
   *
   * @return the suggestions
   */
  List<String> suggestions();
}
