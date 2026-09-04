/*
 * Copyright (C) 2026 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.config;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Reads from an ordered list of layers, the last being the shipped {@code default-velocity.yml}
 * so that every path resolves and no caller passes a fallback. The layer that defines a path
 * converts it, so each layer parses its own storage.
 *
 * <p>Resolution happens per requested path: a leaf is overridden on its own, while a section is
 * taken whole from one layer and never merged with another.</p>
 */
public final class LayeredConfiguration implements Configuration {

  private final List<Configuration> layers;

  public LayeredConfiguration(final Configuration... layers) {
    this.layers = List.of(layers);
  }

  @Override
  public Set<String> keys() {
    return union(Configuration::keys);
  }

  @Override
  public Set<String> paths() {
    return union(Configuration::paths);
  }

  @Override
  public Set<String> leafPaths() {
    return union(Configuration::leafPaths);
  }

  @Override
  public boolean contains(final String path) {
    return layerContaining(path) != null;
  }

  @Override
  public @Nullable Object get(final String path) {
    final Configuration layer = layerContaining(path);
    return layer == null ? null : layer.get(path);
  }

  @Override
  public String getString(final String path) {
    return layerDefining(path).getString(path);
  }

  @Override
  public int getInt(final String path) {
    return layerDefining(path).getInt(path);
  }

  @Override
  public boolean getBoolean(final String path) {
    return layerDefining(path).getBoolean(path);
  }

  @Override
  public List<String> getStringList(final String path) {
    return layerDefining(path).getStringList(path);
  }

  @Override
  public <T extends Enum<T>> T getEnum(final String path, final Class<T> type) {
    return layerDefining(path).getEnum(path, type);
  }

  @Override
  public Configuration getSection(final String path) {
    return layerDefining(path).getSection(path);
  }

  private Configuration layerDefining(final String path) {
    final Configuration layer = layerContaining(path);
    if (layer == null) {
      throw new IllegalStateException("No configuration option '" + path
          + "'. Every option must have a default in default-velocity.yml.");
    }
    return layer;
  }

  private @Nullable Configuration layerContaining(final String path) {
    for (final Configuration layer : layers) {
      if (layer.contains(path)) {
        return layer;
      }
    }
    return null;
  }

  private Set<String> union(final Function<Configuration, Set<String>> of) {
    final Set<String> union = new LinkedHashSet<>();
    for (final Configuration layer : layers) {
      union.addAll(of.apply(layer));
    }
    return union;
  }
}
