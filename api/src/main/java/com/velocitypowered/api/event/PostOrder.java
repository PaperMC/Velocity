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
public enum PostOrder {

  /**
   * Indicates the listener should be invoked first, before any other listener.
   * This order is suitable for listeners that must handle the event before others.
   */
  FIRST,
  /**
   * Indicates the listener should be invoked early, but after listeners with {@link #FIRST}.
   * This order is suitable for handling the event before most other listeners.
   */
  EARLY,
  /**
   * Indicates the listener should be invoked in the normal order of execution.
   * This is the default and most commonly used order.
   */
  NORMAL,
  /**
   * Indicates the listener should be invoked later in the execution order,
   * after listeners with {@link #NORMAL}.
   * This order is suitable for listeners that should observe the results of
   * earlier listeners.
   */
  LATE,
  /**
   * Indicates the listener should be invoked last, after all other listeners.
   * This order is suitable for listeners that should run only after all others
   * have completed handling the event.
   */
  LAST,

  /**
   * Previously used to specify that {@link Subscribe#priority()} should be used.
   *
   * @deprecated No longer required, you only need to specify {@link Subscribe#priority()}.
   */
  @Deprecated
  CUSTOM

}
