package com.velocitypowered.api.event;

import javax.annotation.Nonnull;
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
    void register(@Nonnull Object plugin, @Nonnull Object listener);

    /**
     * Requests that the specified {@code listener} listen for events and associate it with the {@code plugin}.
     * @param plugin the plugin to associate with the listener
     * @param eventClass the class for the event listener to register
     * @param listener the listener to register
     */
    <E> void register(@Nonnull Object plugin, @Nonnull Class<E> eventClass, @Nonnull EventHandler<E> listener);

    /**
     * Posts the specified event to the event bus asynchronously. This allows Velocity to continue servicing connections
     * while a plugin handles a potentially long-running operation such as a database query.
     * @param event the event to post
     * @return a {@link CompletableFuture} representing the posted event
     */
    CompletableFuture<Object> post(@Nonnull Object event);

    /**
     * Posts the specified event to the event bus and discards the result.
     * @param event the event to post
     */
    default void fireAndForget(@Nonnull Object event) {
        post(event);
    }

    /**
     * Unregisters all listeners for the specified {@code plugin}.
     * @param plugin the plugin to deregister listeners for
     */
    void unregisterPluginListeners(@Nonnull Object plugin);

    /**
     * Unregisters a specific listener for a specific plugin.
     * @param plugin the plugin associated with the listener
     * @param listener the listener to deregister
     */
    void unregisterListener(@Nonnull Object plugin, @Nonnull Object listener);

    /**
     * Unregisters a specific listener for a specific plugin.
     * @param plugin the plugin to associate with the listener
     * @param listener the listener to register
     */
    <E> void unregister(@Nonnull Object plugin, @Nonnull EventHandler<E> listener);
}
