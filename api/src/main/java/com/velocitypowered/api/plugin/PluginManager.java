/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.plugin;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.Collection;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Manages plugins loaded on the proxy. This manager can retrieve {@link PluginContainer}s from
 * plugin instances and inject arbitrary JAR files into the plugin classpath with {@link
 * #addToClasspath(Object, Path)}.
 */
public interface PluginManager {

  /**
   * Gets the plugin container from an instance.
   *
   * @param instance the instance
   * @return the container
   */
  @Nullable PluginContainer fromInstance(Object instance);

  /**
   * Retrieves a {@link PluginContainer} based on its ID.
   *
   * @param id the plugin ID
   * @return the plugin, if available
   */
  @Nullable PluginContainer getPlugin(String id);

  /**
   * Gets a {@link Collection} of all {@link PluginContainer}s.
   *
   * @return the plugins
   */
  Collection<PluginContainer> plugins();

  /**
   * Checks if a plugin is loaded based on its ID.
   *
   * @param id the id of the plugin
   * @return {@code true} if loaded
   */
  boolean isLoaded(String id);

  /**
   * Adds the specified {@code path} to the plugin classpath.
   *
   * @param plugin the plugin
   * @param path the path to the JAR you want to inject into the classpath
   * @throws UnsupportedOperationException if the operation is not applicable to this plugin
   */
  void addToClasspath(Object plugin, Path path);

  default PluginContainer ensurePluginContainer(Object plugin) {
    requireNonNull(plugin, "plugin");
    PluginContainer container = fromInstance(plugin);
    if (container == null) {
      throw new IllegalArgumentException("Specified plugin is not loaded");
    }
    return container;
  }
}
