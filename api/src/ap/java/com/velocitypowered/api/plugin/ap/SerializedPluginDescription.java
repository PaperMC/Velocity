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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Serialized version of {@code com.velocitypowered.api.plugin.PluginDescription}.
 */
public final class SerializedPluginDescription {
  public static final Pattern ID_PATTERN = Pattern.compile("[a-z][a-z0-9-_]{0,63}");

  static final String PLUGIN_ID = "id";
  static final String PLUGIN_NAME = "name";
  static final String PLUGIN_VERSION = "version";
  static final String PLUGIN_DESCRIPTION = "description";
  static final String PLUGIN_URL = "url";
  static final String PLUGIN_AUTHORS = "authors";
  static final String PLUGIN_DEPENDENCIES = "dependencies";
  static final String DEPENDENCY_ID = "id";
  static final String DEPENDENCY_OPTIONAL = "optional";

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

  static SerializedPluginDescription from(
      PluginProcessingEnvironment.AnnotationWrapper plugin,
      String qualifiedName
  ) {
    List<Dependency> dependencies = new ArrayList<>();
    for (final PluginProcessingEnvironment.AnnotationWrapper dependency :
        plugin.getList(PLUGIN_DEPENDENCIES, PluginProcessingEnvironment.AnnotationWrapper.class)) {
      dependencies.add(new Dependency(
          dependency.get(DEPENDENCY_ID, String.class),
          dependency.get(DEPENDENCY_OPTIONAL, Boolean.class)
      ));
    }
    return new SerializedPluginDescription(
        plugin.get(PLUGIN_ID, String.class),
        plugin.get(PLUGIN_NAME, String.class),
        plugin.get(PLUGIN_VERSION, String.class),
        plugin.get(PLUGIN_DESCRIPTION, String.class),
        plugin.get(PLUGIN_URL, String.class),
        plugin.getList(PLUGIN_AUTHORS, String.class).stream().filter(author -> !author.isEmpty())
            .collect(Collectors.toList()), dependencies, qualifiedName);
  }

  public String getId() {
    return id;
  }

  public @Nullable String getName() {
    return name;
  }

  public @Nullable String getVersion() {
    return version;
  }

  public @Nullable String getDescription() {
    return description;
  }

  public @Nullable String getUrl() {
    return url;
  }

  public List<String> getAuthors() {
    return authors == null ? ImmutableList.of() : authors;
  }

  public List<Dependency> getDependencies() {
    return dependencies == null ? ImmutableList.of() : dependencies;
  }

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

    public Dependency(String id, boolean optional) {
      this.id = id;
      this.optional = optional;
    }

    public String getId() {
      return id;
    }

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
