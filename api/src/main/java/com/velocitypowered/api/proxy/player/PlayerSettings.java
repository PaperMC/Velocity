/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.player;

import java.util.Locale;

/**
 * Represents the client settings for the player.
 */
public interface PlayerSettings {

  /**
   * Returns the locale of the Minecraft client.
   *
   * @return the client locale
   */
  Locale getLocale();

  /**
   * Returns the client's view distance. This does not guarantee the client will see this many
   * chunks, since your servers are responsible for sending the chunks.
   *
   * @return the client view distance
   */
  byte getViewDistance();

  /**
   * Returns the chat setting for the client.
   *
   * @return the chat setting
   */
  ChatMode getChatMode();

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
  SkinParts getSkinParts();

  /**
   * Returns the primary hand of the client.
   *
   * @return the primary hand of the client
   */
  MainHand getMainHand();

  /**
   * Returns whether the client explicitly allows listing on the
   * {@link com.velocitypowered.api.proxy.player.TabList} or not in
   * anonymous TabList mode.
   * This feature was introduced in 1.18.
   *
   * @return whether or not the client explicitly allows listing. Always false on older clients.
   * @sinceMinecraft 1.18
   */
  boolean isClientListingAllowed();

  /**
   * Returns if the client has text filtering enabled.
   *
   * @return if text filtering is enabled
   */
  boolean isTextFilteringEnabled();

  /**
   * Returns the selected "Particles" option state.
   *
   * @return the particle option
   */
  ParticleStatus getParticleStatus();

  /**
   * The client's current chat display mode.
   */
  enum ChatMode {
    /**
     * Chat is fully visible.
     */
    SHOWN,
    /**
     * Only command messages are shown.
     */
    COMMANDS_ONLY,
    /**
     * Chat is completely hidden.
     */
    HIDDEN
  }

  /**
   * The player's selected dominant hand.
   */
  enum MainHand {
    /**
     * This scope defines the left hand.
     */
    LEFT,
    /**
     * This scope defines the right hand.
     */
    RIGHT
  }

  /**
   * The client's current "Particles" option state.
   */
  enum ParticleStatus {
    /**
     * All particles are shown.
     */
    ALL,
    /**
     * A reduced number of particles are shown.
     */
    DECREASED,
    /**
     * Minimal particle effects are shown.
     */
    MINIMAL
  }
}
