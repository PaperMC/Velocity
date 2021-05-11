/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event;

/**
 * Allows a listener to receive direct dispatches of events. This interface can be used directly
 * by a listener (using {@link EventManager#register(Object, Class, short, EventHandler)} or
 * similar), or pass events through to an external system to be handled.
 */
@FunctionalInterface
public interface EventHandler<E> {

  EventTask execute(E event);
}
