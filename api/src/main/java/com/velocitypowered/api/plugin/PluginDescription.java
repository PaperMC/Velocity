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
import java.util.Optional;
import java.util.regex.Pattern;

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
   * @return an {@link Optional} with the plugin name, may be empty
   * @see Plugin#name()
   */
  default Optional<String> name() {
    return Optional.empty();
  }

  /**
   * Gets the version of the {@link Plugin} within this container.
   *
   * @return an {@link Optional} with the plugin version, may be empty
   * @see Plugin#version()
   */
  default Optional<String> version() {
    return Optional.empty();
  }

  /**
   * Gets the description of the {@link Plugin} within this container.
   *
   * @return an {@link Optional} with the plugin description, may be empty
   * @see Plugin#description()
   */
  default Optional<String> description() {
    return Optional.empty();
  }

  /**
   * Gets the url or website of the {@link Plugin} within this container.
   *
   * @return an {@link Optional} with the plugin url, may be empty
   * @see Plugin#url()
   */
  default Optional<String> url() {
    return Optional.empty();
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

  default Optional<PluginDependency> getDependency(String id) {
    return Optional.empty();
  }

  /**
   * Returns the file path the plugin was loaded from.
   *
   * @return the path the plugin was loaded from or {@link Optional#empty()} if unknown
   */
  default Optional<Path> file() {
    return Optional.empty();
  }
}
