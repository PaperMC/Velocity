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

  BLACK('0'),
  DARK_BLUE('1'),
  DARK_GREEN('2'),
  DARK_AQUA('3'),
  DARK_RED('4'),
  DARK_PURPLE('5'),
  GOLD('6'),
  GRAY('7'),
  DARK_GRAY('8'),
  BLUE('9'),
  GREEN('a'),
  AQUA('b'),
  RED('c'),
  LIGHT_PURPLE('d'),
  YELLOW('e'),
  WHITE('f'),

  OBFUSCATED('k'),
  BOLD('l'),
  STRIKETHROUGH('m'),
  UNDERLINE('n'),
  ITALIC('o'),

  RESET('r');

  private static final String COLOR_CHAR = "§";
  private final char colorCode;

  TeamColor(char colorCode) {
    this.colorCode = colorCode;
  }

  public char getColorCode() {
    return colorCode;
  }

  @Override
  public String toString() {
    return COLOR_CHAR + colorCode;
  }
}
