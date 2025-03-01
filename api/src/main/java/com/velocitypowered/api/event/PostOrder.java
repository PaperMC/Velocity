/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the order an event will be posted to a listener method, relative to other listeners.
 *
 * <p>Listeners are called in following order: {@link #FIRST} -> {@link #EARLY} ->
 * {@link #NORMAL} -> {@link #LATE} -> {@link #LAST}</p>
 */
public interface PostOrder {

  short FIRST = Short.MAX_VALUE - 1;
  short EARLY = Short.MAX_VALUE / 2;
  short NORMAL = 0;
  short LATE = Short.MIN_VALUE / 2;
  short LAST = Short.MIN_VALUE + 1;

  /**
   * Previously used to specify that {@link Subscribe#priority()} should be used.
   *
   * @deprecated No longer required, you only need to specify {@link Subscribe#priority()}.
   */
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "3.6.0")
  short CUSTOM = 0;

  /**
   * Only for backwards compatibility.
   *
   * @deprecated It is necessary only for backwards compatibility. Do not use this method.
   */
  @Deprecated(forRemoval = true)
  @ApiStatus.ScheduledForRemoval(inVersion = "3.6.0")
  int priority();

  /**
   * Only for backwards compatibility.
   *
   * @return PostOrder values
   * @deprecated It is necessary only for backwards compatibility. Do not use this method.
   */
  @Deprecated(forRemoval = true)
  @ApiStatus.ScheduledForRemoval(inVersion = "3.6.0")
  static PostOrder[] values() {
    return new PostOrder[] {
            () -> FIRST,
            () -> EARLY,
            () -> NORMAL,
            () -> LATE,
            () -> LAST,
            () -> CUSTOM
    };
  }

  /**
   * Only for backwards compatibility.
   *
   * @param name PostOrder constant name
   * @return PostOrder with specified name
   * @deprecated It is necessary only for backwards compatibility. Do not use this method.
   */
  @Deprecated(forRemoval = true)
  @ApiStatus.ScheduledForRemoval(inVersion = "3.6.0")
  static PostOrder valueOf(@NotNull String name) {
    Preconditions.checkNotNull(name, "PostOrder name cannot be null");
    return switch (name.toUpperCase()) {
      case "FIRST" -> () -> FIRST;
      case "EARLY" -> () -> EARLY;
      case "NORMAL" -> () -> NORMAL;
      case "LAST" -> () -> LAST;
      case "CUSTOM" -> () -> CUSTOM;
      default -> throw new IllegalArgumentException("No PostOrder found with the name " + name);
    };
  }

}
