/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event;

/**
 * Represents the order an event will be posted to a listener method, relative to other listeners.
 */
public class PostOrder {

  private PostOrder() {

  }

  public static final short FIRST = -32767;
  public static final short EARLY = -16384;
  public static final short NORMAL = 0;
  public static final short LATE = 16834;
  public static final short LAST = 32767;

}
