package com.velocitypowered.api.event;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.concurrent.CompletableFuture;

/**
 * Allows plugins to register and deregister listeners for event handlers.
 */
public interface EventManager {
    /**
     * Requests that the specified {@code listener} listen for events and associate it with the {@code plugin}.
     * @param plugin the plugin to associate with the listener
     * @param listener the listener to register
     */
    void register(@NonNull Object plugin, @NonNull Object listener);

    /**
     * Requests that the specified {@code handler} listen for events and associate it with the {@code plugin}.
     * @param plugin the plugin to associate with the handler
     * @param eventClass the class for the event handler to register
     * @param handler the handler to register
     * @param <E> the event type to handle
     */
    <E> void register(@NonNull Object plugin, @NonNull Class<E> eventClass, @NonNull EventHandler<E> handler);

    /**
     * Fires the specified event to the event bus asynchronously. This allows Velocity to continue servicing connections
     * while a plugin handles a potentially long-running operation such as a database query.
     * @param event the event to fire
     * @return a {@link CompletableFuture} representing the posted event
     */
    @NonNull <E> CompletableFuture<E> fire(@NonNull E event);

    /**
     * Posts the specified event to the event bus and discards the result.
     * @param event the event to fire
     */
    default void fireAndForget(@NonNull Object event) {
        fire(event);
    }

    /**
     * Unregisters all listeners for the specified {@code plugin}.
     * @param plugin the plugin to deregister listeners for
     */
    void unregisterListeners(@NonNull Object plugin);

    /**
     * Unregisters a specific listener for a specific plugin.
     * @param plugin the plugin associated with the listener
     * @param listener the listener to deregister
     */
    void unregisterListener(@NonNull Object plugin, @NonNull Object listener);

    /**
     * Unregisters a specific event handler for a specific plugin.
     * @param plugin the plugin to associate with the handler
     * @param handler the handler to register
     * @param <E> the event type to handle
     */
    <E> void unregister(@NonNull Object plugin, @NonNull EventHandler<E> handler);
}
