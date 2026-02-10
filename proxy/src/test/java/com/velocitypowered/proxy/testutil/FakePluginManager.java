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
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.proxy.plugin.virtual.VelocityVirtualPlugin;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A fake plugin manager.
 */
public class FakePluginManager implements PluginManager {

  public static final Object PLUGIN_A = new Object();
  public static final Object PLUGIN_B = new Object();

  private final PluginContainer containerA = new FakePluginContainer("a", PLUGIN_A);
  private final PluginContainer containerB = new FakePluginContainer("b", PLUGIN_B);
  private final PluginContainer containerVelocity = new FakePluginContainer("velocity",
      VelocityVirtualPlugin.INSTANCE);

  private ExecutorService service = Executors.newCachedThreadPool(
      new ThreadFactoryBuilder().setNameFormat("Test Async Thread").setDaemon(true).build()
  );

  @Override
  public @NonNull Optional<PluginContainer> fromInstance(@NonNull Object instance) {
    if (instance == PLUGIN_A) {
      return Optional.of(containerA);
    } else if (instance == PLUGIN_B) {
      return Optional.of(containerB);
    } else if (instance == VelocityVirtualPlugin.INSTANCE) {
      return Optional.of(containerVelocity);
    } else {
      return Optional.empty();
    }
  }

  @Override
  public @NonNull Optional<PluginContainer> getPlugin(@NonNull String id) {
    return switch (id) {
      case "a" -> Optional.of(containerA);
      case "b" -> Optional.of(containerB);
      case "velocity" -> Optional.of(containerVelocity);
      default -> Optional.empty();
    };
  }

  @Override
  public @NonNull Collection<PluginContainer> getPlugins() {
    return ImmutableList.of(containerVelocity, containerA, containerB);
  }

  @Override
  public boolean isLoaded(@NonNull String id) {
    return id.equals("a") || id.equals("b");
  }

  @Override
  public void addToClasspath(@NonNull Object plugin, @NonNull Path path) {
    throw new UnsupportedOperationException();
  }

  public void shutdown() {
    this.service.shutdownNow();
  }

  private class FakePluginContainer implements PluginContainer {

    private final String id;
    private final Object instance;

    private FakePluginContainer(String id, Object instance) {
      this.id = id;
      this.instance = instance;
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
