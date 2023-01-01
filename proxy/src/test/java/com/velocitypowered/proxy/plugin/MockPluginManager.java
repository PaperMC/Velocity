/*
 * Copyright (C) 2020-2023 Velocity Contributors
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

package com.velocitypowered.proxy.plugin;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginManager;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

/**
 * Mock of {@link PluginManager}.
 */
public class MockPluginManager implements PluginManager {

  public static final PluginManager INSTANCE = new MockPluginManager();

  @Override
  public Optional<PluginContainer> fromInstance(final Object instance) {
    return Optional.empty();
  }

  @Override
  public Optional<PluginContainer> getPlugin(final String id) {
    return Optional.empty();
  }

  @Override
  public Collection<PluginContainer> getPlugins() {
    return ImmutableList.of();
  }

  @Override
  public boolean isLoaded(final String id) {
    return false;
  }

  @Override
  public void addToClasspath(final Object plugin, final Path path) {

  }
}
