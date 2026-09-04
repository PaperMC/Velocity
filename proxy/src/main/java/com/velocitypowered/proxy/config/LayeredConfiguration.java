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
    final Set<String> keys = new LinkedHashSet<>();
    for (final Configuration layer : layers) {
      keys.addAll(layer.keys());
    }
    return keys;
  }

  @Override
  public boolean contains(final String path) {
    for (final Configuration layer : layers) {
      if (layer.contains(path)) {
        return true;
      }
    }
    return false;
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
  public ConfigurationSection getSection(final String path) {
    return layerDefining(path).getSection(path);
  }

  private Configuration layerDefining(final String path) {
    for (final Configuration layer : layers) {
      if (layer.contains(path)) {
        return layer;
      }
    }
    throw new IllegalStateException("No configuration option '" + path
        + "'. Every option must have a default in default-velocity.yml.");
  }
}
