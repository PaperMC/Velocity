/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event;

/**
 * Provides convenient shorthands for represents the order an event will be posted to a listener.
 */
public enum PostOrder {
  ;

  public static final short FIRST = Short.MIN_VALUE;
  public static final short EARLY = Short.MIN_VALUE >> 1;
  public static final short NORMAL = 0;
  public static final short LATE = Short.MAX_VALUE >> 1;
  public static final short LAST = Short.MAX_VALUE;

}
