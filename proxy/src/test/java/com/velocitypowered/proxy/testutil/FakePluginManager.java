/*
 * Copyright (C) 2018-2023 Velocity Contributors
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
import com.velocitypowered.proxy.plugin.loader.VelocityPluginContainer;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A fake plugin manager.
 */
public class FakePluginManager implements PluginManager {

  public static final Object PLUGIN_A = new Object();
  public static final Object PLUGIN_B = new Object();

  private static final PluginContainer PC_A = new FakePluginContainer("a", PLUGIN_A);
  private static final PluginContainer PC_B = new FakePluginContainer("b", PLUGIN_B);

  @Override
  public @NonNull Optional<PluginContainer> fromInstance(@NonNull Object instance) {
    if (instance == PLUGIN_A) {
      return Optional.of(PC_A);
    } else if (instance == PLUGIN_B) {
      return Optional.of(PC_B);
    } else {
      return Optional.empty();
    }
  }

  @Override
  public @NonNull Optional<PluginContainer> getPlugin(@NonNull String id) {
    return switch (id) {
      case "a" -> Optional.of(PC_A);
      case "b" -> Optional.of(PC_B);
      default -> Optional.empty();
    };
  }

  @Override
  public @NonNull Collection<PluginContainer> getPlugins() {
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

  private static class FakePluginContainer extends VelocityPluginContainer {

    private final String id;
    private final Object instance;
    private final ExecutorService service;

    private FakePluginContainer(String id, Object instance) {
      super(null);
      this.id = id;
      this.instance = instance;
      this.service = ForkJoinPool.commonPool();
    }

    @Override
    public @NonNull PluginDescription getDescription() {
      return () -> id;
    }

    @Override
    public Optional<?> getInstance() {
      return Optional.of(instance);
    }

    @Override
    public ExecutorService getExecutorService() {
      return service;
    }
  }
}
