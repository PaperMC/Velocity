/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.util.title;

/**
 * Provides special-purpose titles.
 *
 * @deprecated Replaced with {@link net.kyori.adventure.title.Title}
 */
@Deprecated
public final class Titles {

  private Titles() {
    throw new AssertionError();
  }

  private static final Title RESET = new Title() {
    @Override
    public String toString() {
      return "reset title";
    }
  };

  private static final Title HIDE = new Title() {
    @Override
    public String toString() {
      return "hide title";
    }
  };

  /**
   * Returns a title that, when sent to the client, will cause all title data to be reset and any
   * existing title to be hidden.
   *
   * @return the reset title
   */
  public static Title reset() {
    return RESET;
  }

  /**
   * Returns a title that, when sent to the client, will cause any existing title to be hidden. The
   * title may be restored by a {@link TextTitle} with no title or subtitle (only a time).
   *
   * @return the hide title
   */
  public static Title hide() {
    return HIDE;
  }

  /**
   * Returns a builder for {@link TextTitle}s.
   *
   * @return a builder for text titles
   */
  public static TextTitle.Builder text() {
    return TextTitle.builder();
  }
}
