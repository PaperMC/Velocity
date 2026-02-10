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
 *
 * @param <E> the event type this handler accepts
 */
@FunctionalInterface
public interface EventHandler<E> {

  /**
   * Executes this handler synchronously with the given event.
   *
   * @param event the event to handle
   */
  void execute(E event);

  /**
   * Executes this handler asynchronously with the given event.
   *
   * <p>If asynchronous handling is not implemented, the event is executed synchronously
   * and this method returns {@code null}.</p>
   *
   * @param event the event to handle
   * @return an {@link EventTask} representing the async task, or {@code null} if not async
   */
  default @Nullable EventTask executeAsync(E event) {
    execute(event);
    return null;
  }
}
