package com.velocitypowered.api.event;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Represents an interface to perform direct dispatch of an event. This makes integration easier to achieve with platforms
 * such as RxJava.
 */
@FunctionalInterface
public interface EventHandler<E> {
    void execute(@NonNull E event);
}
