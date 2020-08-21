package com.velocitypowered.proxy.plugin;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginManager;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

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
