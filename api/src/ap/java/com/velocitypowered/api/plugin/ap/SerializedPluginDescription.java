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
import com.velocitypowered.api.plugin.PluginDescription;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Serialized version of {@link PluginDescription}.
 *
 * @param id @Nullable is used here to make GSON skip these in the serialized file
 */
public record SerializedPluginDescription(String id, @Nullable String name, @Nullable String version, @Nullable String description,
      @Nullable String url, @Nullable List<String> authors, @Nullable List<Dependency> dependencies,
      @Nullable List<String> provides, String main) {

  public static final String ID_PATTERN_STRING = "[a-z][a-z0-9-_]{0,63}";
  public static final Pattern ID_PATTERN = Pattern.compile(ID_PATTERN_STRING);

  /**
   * Constructs a SerializedPluginDescription.
   */
  public SerializedPluginDescription {
    Preconditions.checkNotNull(id, "id");
    Preconditions.checkArgument(ID_PATTERN.matcher(id).matches(), "id is not valid");
    name = Strings.emptyToNull(name);
    version = Strings.emptyToNull(version);
    description = Strings.emptyToNull(description);
    url = Strings.emptyToNull(url);
    authors = authors == null || authors.isEmpty() ? ImmutableList.of() : authors;
    dependencies = dependencies == null || dependencies.isEmpty() ? ImmutableList.of() : dependencies;
    provides = provides == null || provides.isEmpty() ? ImmutableList.of() : provides;
    main = Preconditions.checkNotNull(main, "main");
  }

  static SerializedPluginDescription from(Plugin plugin, String qualifiedName) {
    List<Dependency> dependencies = new ArrayList<>();
    for (com.velocitypowered.api.plugin.Dependency dependency : plugin.dependencies()) {
      dependencies.add(new Dependency(dependency.id(), dependency.optional()));
    }
    return new SerializedPluginDescription(plugin.id(), plugin.name(), plugin.version(),
        plugin.description(), plugin.url(),
        Arrays.stream(plugin.authors()).filter(author -> !author.isEmpty())
            .collect(Collectors.toList()), dependencies,
        Arrays.stream(plugin.provides()).filter(provided -> !provided.isEmpty())
            .collect(Collectors.toList()), qualifiedName);
  }

  @Override
  public List<String> authors() {
    return authors == null ? ImmutableList.of() : authors;
  }

  @Override
  public List<Dependency> dependencies() {
    return dependencies == null ? ImmutableList.of() : dependencies;
  }

  @Override
  public List<String> provides() {
    return provides == null ? ImmutableList.of() : provides;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, version, description, url, authors, dependencies, provides);
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
        + ", provides=" + provides
        + ", main='" + main + '\''
        + '}';
  }

  /**
   * Represents a dependency.
   */
  public record Dependency(String id, boolean optional) {

    @Override
    public String toString() {
      return "Dependency{"
          + "id='" + id + '\''
          + ", optional=" + optional
          + '}';
    }

  }

}
