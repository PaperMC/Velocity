package com.velocitypowered.api.event;

/**
 * Represents the order an event will be posted to a listener method, relative to other listeners.
 */
public enum PostOrder {
  ;

  public static final short FIRST = Short.MIN_VALUE;
  public static final short EARLY = Short.MIN_VALUE >> 1;
  public static final short NORMAL = 0;
  public static final short LATE = Short.MAX_VALUE >> 1;
  public static final short LAST = Short.MAX_VALUE;

}
