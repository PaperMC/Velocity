package com.velocitypowered.api.plugin;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.nio.file.Path;
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
    @NonNull Optional<PluginContainer> fromInstance(@NonNull Object instance);

    /**
     * Retrieves a {@link PluginContainer} based on its ID.
     *
     * @param id the plugin ID
     * @return the plugin, if available
     */
    @NonNull Optional<PluginContainer> getPlugin(@NonNull String id);

    /**
     * Gets a {@link Collection} of all {@link PluginContainer}s.
     *
     * @return the plugins
     */
    @NonNull Collection<PluginContainer> getPlugins();

    /**
     * Checks if a plugin is loaded based on its ID.
     *
     * @param id the id of the {@link Plugin}
     * @return {@code true} if loaded
     */
    boolean isLoaded(@NonNull String id);

    /**
     * Adds the specified {@code path} to the plugin classpath.
     *
     * @param plugin the plugin
     * @param path the path to the JAR you want to inject into the classpath
     * @throws UnsupportedOperationException if the operation is not applicable to this plugin
     */
    void addToClasspath(@NonNull Object plugin, @NonNull Path path);
}
