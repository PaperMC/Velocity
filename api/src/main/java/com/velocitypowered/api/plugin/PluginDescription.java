package com.velocitypowered.api.plugin;

import com.google.common.collect.ImmutableSet;
import com.velocitypowered.api.plugin.meta.PluginDependency;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Represents metadata for a specific version of a plugin.
 */
public interface PluginDescription {
    /**
     * The pattern plugin IDs must match. Plugin IDs may only contain
     * alphanumeric characters, dashes or underscores, must start with
     * an alphabetic character and cannot be longer than 64 characters.
     */
    Pattern ID_PATTERN = Pattern.compile("[A-z][A-z0-9-_]{0,63}");

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
    default Collection<PluginDependency> getDependencies() {
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
}
