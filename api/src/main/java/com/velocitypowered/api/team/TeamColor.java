/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.team;

/**
 * All possible colors which can be applied to a {@link Team}
 * using {@link Team#setColor(TeamColor)} and queried using
 * {@link Team#getColor()}.
 */
public enum TeamColor {

  BLACK,
  DARK_BLUE,
  DARK_GREEN,
  DARK_AQUA,
  DARK_RED,
  DARK_PURPLE,
  GOLD,
  GRAY,
  DARK_GRAY,
  BLUE,
  GREEN,
  AQUA,
  RED,
  LIGHT_PURPLE,
  YELLOW,
  WHITE,

  OBFUSCATED,
  BOLD,
  STRIKETHROUGH,
  UNDERLINE,
  ITALIC,

  RESET,
}
