/*
 * Copyright (C) 2021-2023 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.event;

import com.velocitypowered.api.event.AwaitingEventExecutor;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.EventTask;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Core class for invoking event handlers registered by plugins.
 */
public interface UntargetedEventHandler {

  EventHandler<Object> buildHandler(Object targetInstance);

  /**
   * Interface used for invoking listeners that return {@link EventTask}.
   */
  interface EventTaskHandler extends UntargetedEventHandler {

    @Nullable EventTask execute(Object targetInstance, Object event);

    @Override
    default EventHandler<Object> buildHandler(final Object targetInstance) {
      return (AwaitingEventExecutor<Object>) event -> execute(targetInstance, event);
    }
  }

  /**
   * Interface used for invoking listeners that return nothing.
   */
  interface VoidHandler extends UntargetedEventHandler {

    void execute(Object targetInstance, Object event);

    @Override
    default EventHandler<Object> buildHandler(final Object targetInstance) {
      return (AwaitingEventExecutor<Object>) event -> {
        execute(targetInstance, event);
        return null;
      };
    }
  }

  /**
   * Interface used for invoking listeners that take a {@link Continuation} along with an event.
   */
  interface WithContinuationHandler extends UntargetedEventHandler {

    void execute(Object targetInstance, Object event, Continuation continuation);

    @Override
    default EventHandler<Object> buildHandler(final Object targetInstance) {
      return (AwaitingEventExecutor<Object>) event -> EventTask.withContinuation(continuation ->
          execute(targetInstance, event, continuation));
    }
  }
}