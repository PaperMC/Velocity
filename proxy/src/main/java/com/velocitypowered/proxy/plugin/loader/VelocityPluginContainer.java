package com.velocitypowered.proxy.plugin.loader;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import java.util.Optional;

public class VelocityPluginContainer implements PluginContainer {

  private final PluginDescription description;
  private Object instance;

  public VelocityPluginContainer(PluginDescription description) {
    this.description = description;
  }

  @Override
  public PluginDescription getDescription() {
    return this.description;
  }

  @Override
  public Optional<?> getInstance() {
    return Optional.ofNullable(instance);
  }

  public void setInstance(Object instance) {
    this.instance = instance;
  }
}
