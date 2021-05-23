/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event;

import org.checkerframework.checker.nullness.qual.Nullable;

@FunctionalInterface
public interface AsyncEventExecutor<E> extends EventHandler<E> {

  default void execute(E event) {
    throw new UnsupportedOperationException(
        "This event handler can only be invoked asynchronously.");
  }

  @Nullable EventTask executeAsync(E event);
}
