/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event;

/**
 * Represents an interface to perform direct dispatch of an event. This makes integration easier to
 * achieve with platforms such as RxJava.
 */
@FunctionalInterface
public interface EventHandler<E> {

  void execute(E event);
}
