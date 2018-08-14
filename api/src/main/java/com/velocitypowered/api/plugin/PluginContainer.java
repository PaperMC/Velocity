package com.velocitypowered.api.plugin;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;

/**
 * A wrapper around a plugin loaded by the proxy.
 */
public interface PluginContainer {
    /**
     * Returns the plugin's description.
     *
     * @return the plugin's description
     */
    @NonNull PluginDescription getDescription();

    /**
     * Returns the created plugin if it is available.
     *
     * @return the instance if available
     */
    default Optional<?> getInstance() {
        return Optional.empty();
    }
}
