package com.velocitypowered.api.event;

/**
 * Represents an interface to perform direct dispatch of an event. This makes integration easier to
 * achieve with platforms such as RxJava.
 */
@FunctionalInterface
public interface EventHandler<E> {

  EventTask execute(E event);
}
