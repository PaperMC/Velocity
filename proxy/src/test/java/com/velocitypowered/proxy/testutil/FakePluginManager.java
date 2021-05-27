/*
 * Copyright (C) 2018 Velocity Contributors
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

package com.velocitypowered.proxy.testutil;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.PluginManager;
import java.nio.file.Path;
import java.util.Collection;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class FakePluginManager implements PluginManager {

  public static final Object PLUGIN_A = new Object();
  public static final Object PLUGIN_B = new Object();

  private static final PluginContainer PC_A = new FakePluginContainer("a", PLUGIN_A);
  private static final PluginContainer PC_B = new FakePluginContainer("b", PLUGIN_B);

  @Override
  public @Nullable PluginContainer fromInstance(@NonNull Object instance) {
    if (instance == PLUGIN_A) {
      return PC_A;
    } else if (instance == PLUGIN_B) {
      return PC_B;
    } else {
      return null;
    }
  }

  @Override
  public @Nullable PluginContainer getPlugin(@NonNull String id) {
    switch (id) {
      case "a":
        return PC_A;
      case "b":
        return PC_B;
      default:
        return null;
    }
  }

  @Override
  public @NonNull Collection<PluginContainer> plugins() {
    return ImmutableList.of(PC_A, PC_B);
  }

  @Override
  public boolean isLoaded(@NonNull String id) {
    return id.equals("a") || id.equals("b");
  }

  @Override
  public void addToClasspath(@NonNull Object plugin, @NonNull Path path) {
    throw new UnsupportedOperationException();
  }

  private static class FakePluginContainer implements PluginContainer {

    private final String id;
    private final Object instance;

    private FakePluginContainer(String id, Object instance) {
      this.id = id;
      this.instance = instance;
    }

    @Override
    public @NonNull PluginDescription description() {
      return () -> id;
    }

    @Override
    public Object instance() {
      return instance;
    }
  }
}
