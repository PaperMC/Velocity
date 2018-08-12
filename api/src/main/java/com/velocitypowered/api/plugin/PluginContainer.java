package com.velocitypowered.api.plugin;

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
    PluginDescription getDescription();

    /**
     * Returns the created plugin if it is available.
     *
     * @return the instance if available
     */
    default Optional<?> getInstance() {
        return Optional.empty();
    }
}
