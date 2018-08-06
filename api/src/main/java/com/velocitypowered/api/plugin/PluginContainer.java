package com.velocitypowered.api.plugin;

import com.google.common.collect.ImmutableSet;
import com.velocitypowered.api.plugin.meta.PluginDependency;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A wrapper around a class marked with an {@link Plugin} annotation to
 * retrieve information from the annotation for easier use.
 */
public interface PluginContainer {
    /**
     * Gets the qualified ID of the {@link Plugin} within this container.
     *
     * @return the plugin ID
     * @see Plugin#id()
     */
    String getId();

    /**
     * Gets the version of the {@link Plugin} within this container.
     *
     * @return the plugin version, or {@link Optional#empty()} if unknown
     * @see Plugin#version()
     */
    default Optional<String> getVersion() {
        return Optional.empty();
    }

    /**
     * Gets the author of the {@link Plugin} within this container.
     *
     * @return the plugin author, or {@link Optional#empty()} if unknown
     * @see Plugin#author()
     */
    default Optional<String> getAuthor() {
        return Optional.empty();
    }

    /**
     * Gets a {@link Set} of all dependencies of the {@link Plugin} within
     * this container.
     *
     * @return the plugin dependencies, can be empty
     * @see Plugin#dependencies()
     */
    default Set<PluginDependency> getDependencies() {
        return ImmutableSet.of();
    }

    default Optional<PluginDependency> getDependency(String id) {
        return Optional.empty();
    }

    /**
     * Returns the source the plugin was loaded from.
     *
     * @return the source the plugin was loaded from or {@link Optional#empty()}
     *         if unknown
     */
    default Optional<Path> getSource() {
        return Optional.empty();
    }

    /**
     * Returns the created instance of {@link Plugin} if it is available.
     *
     * @return the instance if available
     */
    default Optional<?> getInstance() {
        return Optional.empty();
    }

    /**
     * Returns the assigned logger to this {@link Plugin}.
     *
     * @return the assigned logger, or {@code null} if unknown.
     */
    default Logger getLogger() {
        return null;
    }
}
