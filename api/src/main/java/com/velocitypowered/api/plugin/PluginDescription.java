/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.plugin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.velocitypowered.api.plugin.meta.PluginDependency;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents metadata for a specific version of a plugin.
 */
public interface PluginDescription {

  /**
   * The pattern plugin IDs must match. Plugin IDs may only contain alphanumeric characters, dashes
   * or underscores, must start with an alphabetic character and cannot be longer than 64
   * characters.
   */
  Pattern ID_PATTERN = Pattern.compile("[a-z][a-z0-9-_]{0,63}");

  /**
   * Gets the qualified ID of the {@link Plugin} within this container.
   *
   * @return the plugin ID
   * @see Plugin#id()
   */
  String id();

  /**
   * Gets the name of the {@link Plugin} within this container.
   *
   * @return a String with the plugin name or the plugin ID
   * @see Plugin#name()
   */
  default String name() {
    return id();
  }

  /**
   * Gets the version of the {@link Plugin} within this container.
   *
   * @return a String with the plugin version, may be null
   * @see Plugin#version()
   */
  default @Nullable String version() {
    return null;
  }

  /**
   * Gets the description of the {@link Plugin} within this container.
   *
   * @return a String with the plugin description, may be null
   * @see Plugin#description()
   */
  default @Nullable String description() {
    return null;
  }

  /**
   * Gets the url or website of the {@link Plugin} within this container.
   *
   * @return an String with the plugin url, may be null
   * @see Plugin#url()
   */
  default @Nullable String url() {
    return null;
  }

  /**
   * Gets the authors of the {@link Plugin} within this container.
   *
   * @return the plugin authors, may be empty
   * @see Plugin#authors()
   */
  default List<String> authors() {
    return ImmutableList.of();
  }

  /**
   * Gets a {@link Collection} of all dependencies of the {@link Plugin} within this container.
   *
   * @return the plugin dependencies, can be empty
   * @see Plugin#dependencies()
   */
  default Collection<PluginDependency> dependencies() {
    return ImmutableSet.of();
  }

  default @Nullable PluginDependency getDependency(String id) {
    return null;
  }

  /**
   * Returns the file path the plugin was loaded from.
   *
   * @return the path the plugin was loaded from or {@code null} if unknown
   */
  default @Nullable Path file() {
    return null;
  }
}
