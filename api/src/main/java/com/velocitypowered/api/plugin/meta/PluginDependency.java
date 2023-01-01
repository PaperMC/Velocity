/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.plugin.meta;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;

import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a dependency on another plugin.
 */
public final class PluginDependency {

  private final String id;
  private final @Nullable String version;

  private final boolean optional;

  /**
   * Creates a new instance.
   *
   * @param id the plugin ID
   * @param version an optional version
   * @param optional whether or not this dependency is optional
   */
  public PluginDependency(String id, @Nullable String version, boolean optional) {
    this.id = checkNotNull(id, "id");
    checkArgument(!id.isEmpty(), "id cannot be empty");
    this.version = emptyToNull(version);
    this.optional = optional;
  }

  /**
   * Returns the plugin ID of this {@link PluginDependency}.
   *
   * @return the plugin ID
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the version this {@link PluginDependency} should match.
   *
   * @return an {@link Optional} with the plugin version, may be empty
   */
  public Optional<String> getVersion() {
    return Optional.ofNullable(version);
  }

  /**
   * Returns whether the dependency is optional for the plugin to work correctly.
   *
   * @return true if dependency is optional
   */
  public boolean isOptional() {
    return optional;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PluginDependency that = (PluginDependency) o;
    return optional == that.optional
        && Objects.equals(id, that.id)
        && Objects.equals(version, that.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, version, optional);
  }

  @Override
  public String toString() {
    return "PluginDependency{"
        + "id='" + id + '\''
        + ", version='" + version + '\''
        + ", optional=" + optional
        + '}';
  }
}
