/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents an interface to perform direct dispatch of an event. This makes integration easier to
 * achieve with platforms such as RxJava. While this interface can be used to implement an awaiting
 * event handler, {@link AwaitingEventExecutor} provides a more idiomatic means to doing so.
 */
@FunctionalInterface
public interface EventHandler<E> {

  void execute(E event);

  default @Nullable EventTask executeAsync(E event) {
    execute(event);
    return null;
  }
}
