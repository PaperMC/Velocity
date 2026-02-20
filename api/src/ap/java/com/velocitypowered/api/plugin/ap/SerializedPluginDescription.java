/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.plugin.ap;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.plugin.Plugin;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Serialized version of {@link com.velocitypowered.api.plugin.PluginDescription}.
 */
public final class SerializedPluginDescription {

  /**
   * The string pattern used to validate plugin IDs.
   *
   * <p>Plugin IDs must start with a lowercase letter and may contain lowercase letters,
   * digits, hyphens, and underscores. The total length must not exceed 64 characters.</p>
   */
  public static final String ID_PATTERN_STRING = "[a-z][a-z0-9-_]{0,63}";
  /**
   * The compiled pattern used to validate plugin IDs.
   *
   * <p>Plugin IDs must start with a lowercase letter and may contain lowercase letters,
   * digits, hyphens, and underscores. The total length must not exceed 64 characters.</p>
   */
  public static final Pattern ID_PATTERN = Pattern.compile(ID_PATTERN_STRING);

  // @Nullable is used here to make GSON skip these in the serialized file
  private final String id;
  private final @Nullable String name;
  private final @Nullable String version;
  private final @Nullable String description;
  private final @Nullable String url;
  private final @Nullable List<String> authors;
  private final @Nullable List<Dependency> dependencies;
  private final String main;

  private SerializedPluginDescription(String id, String name, String version, String description,
      String url,
      List<String> authors, List<Dependency> dependencies, String main) {
    Preconditions.checkNotNull(id, "id");
    Preconditions.checkArgument(ID_PATTERN.matcher(id).matches(), "id is not valid");
    this.id = id;
    this.name = Strings.emptyToNull(name);
    this.version = Strings.emptyToNull(version);
    this.description = Strings.emptyToNull(description);
    this.url = Strings.emptyToNull(url);
    this.authors = authors == null || authors.isEmpty() ? ImmutableList.of() : authors;
    this.dependencies =
        dependencies == null || dependencies.isEmpty() ? ImmutableList.of() : dependencies;
    this.main = Preconditions.checkNotNull(main, "main");
  }

  static SerializedPluginDescription from(Plugin plugin, String qualifiedName) {
    List<Dependency> dependencies = new ArrayList<>();
    for (com.velocitypowered.api.plugin.Dependency dependency : plugin.dependencies()) {
      dependencies.add(new Dependency(dependency.id(), dependency.optional()));
    }
    return new SerializedPluginDescription(plugin.id(), plugin.name(), plugin.version(),
        plugin.description(), plugin.url(),
        Arrays.stream(plugin.authors()).filter(author -> !author.isEmpty())
            .collect(Collectors.toList()), dependencies, qualifiedName);
  }

  /**
   * Gets the ID of the plugin this dependency refers to.
   *
   * @return the plugin ID
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the human-readable name of the plugin.
   *
   * @return the plugin's name, or {@code null} if not specified
   */
  public @Nullable String getName() {
    return name;
  }

  /**
   * Gets the version string of the plugin.
   *
   * @return the plugin version, or {@code null} if not specified
   */
  public @Nullable String getVersion() {
    return version;
  }

  /**
   * Gets the plugin's description, typically a short summary of its functionality.
   *
   * @return the description, or {@code null} if not specified
   */
  public @Nullable String getDescription() {
    return description;
  }

  /**
   * Gets the website URL for the plugin.
   *
   * <p>This is often used to link to documentation, support, or the plugin's homepage.</p>
   *
   * @return the plugin URL, or {@code null} if not specified
   */
  public @Nullable String getUrl() {
    return url;
  }

  /**
   * Gets the list of authors who contributed to the plugin.
   *
   * @return an immutable list of authors; empty if none were specified
   */
  public List<String> getAuthors() {
    return authors == null ? ImmutableList.of() : authors;
  }

  /**
   * Gets the list of declared dependencies for the plugin.
   *
   * <p>Dependencies may be required or optional and describe other plugins, this one depends.</p>
   *
   * @return an immutable list of plugin dependencies
   */
  public List<Dependency> getDependencies() {
    return dependencies == null ? ImmutableList.of() : dependencies;
  }

  /**
   * Gets the fully qualified name of the plugin's main class.
   *
   * @return the main class name
   */
  public String getMain() {
    return main;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SerializedPluginDescription that = (SerializedPluginDescription) o;
    return Objects.equals(id, that.id)
        && Objects.equals(name, that.name)
        && Objects.equals(version, that.version)
        && Objects.equals(description, that.description)
        && Objects.equals(url, that.url)
        && Objects.equals(authors, that.authors)
        && Objects.equals(dependencies, that.dependencies)
        && Objects.equals(main, that.main);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, version, description, url, authors, dependencies);
  }

  @Override
  public String toString() {
    return "SerializedPluginDescription{"
        + "id='" + id + '\''
        + ", name='" + name + '\''
        + ", version='" + version + '\''
        + ", description='" + description + '\''
        + ", url='" + url + '\''
        + ", authors=" + authors
        + ", dependencies=" + dependencies
        + ", main='" + main + '\''
        + '}';
  }

  /**
   * Represents a dependency.
   */
  public static final class Dependency {

    private final String id;
    private final boolean optional;

    /**
     * Constructs a new dependency class.
     *
     * @param id the ID of the dependent plugin
     * @param optional whether the dependency is optional
     */
    public Dependency(String id, boolean optional) {
      this.id = id;
      this.optional = optional;
    }

    /**
     * Gets the ID of the plugin this dependency refers to.
     *
     * @return the plugin ID
     */
    public String getId() {
      return id;
    }

    /**
     * Indicates whether this dependency is optional.
     *
     * <p>Optional dependencies are not required for the plugin to load.</p>
     *
     * @return {@code true} if the dependency is optional; {@code false} otherwise
     */
    public boolean isOptional() {
      return optional;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Dependency that = (Dependency) o;
      return optional == that.optional
          && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, optional);
    }

    @Override
    public String toString() {
      return "Dependency{"
          + "id='" + id + '\''
          + ", optional=" + optional
          + '}';
    }
  }
}
