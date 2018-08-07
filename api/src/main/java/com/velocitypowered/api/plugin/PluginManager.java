package com.velocitypowered.api.plugin;

import java.util.Collection;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * The class that manages plugins. This manager can retrieve
 * {@link PluginContainer}s from {@link Plugin} instances, getting
 * {@link Logger}s, etc.
 */
public interface PluginManager {
    /**
     * Gets the plugin container from an instance.
     *
     * @param instance the instance
     * @return the container
     */
    Optional<PluginContainer> fromInstance(Object instance);

    /**
     * Retrieves a {@link PluginContainer} based on its ID.
     *
     * @param id the plugin ID
     * @return the plugin, if available
     */
    Optional<PluginContainer> getPlugin(String id);

    /**
     * Gets a {@link Collection} of all {@link PluginContainer}s.
     *
     * @return the plugins
     */
    Collection<PluginContainer> getPlugins();

    /**
     * Checks if a plugin is loaded based on its ID.
     *
     * @param id the id of the {@link Plugin}
     * @return {@code true} if loaded
     */
    boolean isLoaded(String id);

}
