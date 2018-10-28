package com.velocitypowered.api.event;

/**
 * Represents the order an event will be posted to a listener method, relative to other listeners.
 */
public enum PostOrder {

  FIRST, EARLY, NORMAL, LATE, LAST;

}
