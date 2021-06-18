/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.player.java;

import java.util.Locale;

/**
 * Represents the client settings for the player.
 */
public interface JavaClientSettings {

  /**
   * Returns the locale of the Minecraft client.
   *
   * @return the client locale
   */
  Locale locale();

  /**
   * Returns the client's view distance. This does not guarantee the client will see this many
   * chunks, since your servers are responsible for sending the chunks.
   *
   * @return the client view distance
   */
  byte viewDistance();

  /**
   * Returns the chat setting for the client.
   *
   * @return the chat setting
   */
  ChatMode chatMode();

  /**
   * Returns whether or not the client has chat colors disabled.
   *
   * @return whether or not the client has chat colors disabled
   */
  boolean hasChatColors();

  /**
   * Returns the parts of player skins the client will show.
   *
   * @return the skin parts for the client
   */
  SkinParts skinParts();

  /**
   * Returns the primary hand of the client.
   *
   * @return the primary hand of the client
   */
  MainHand mainHand();

  enum ChatMode {
    SHOWN,
    COMMANDS_ONLY,
    HIDDEN
  }

  enum MainHand {
    LEFT,
    RIGHT
  }
}
