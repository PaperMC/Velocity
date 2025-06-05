/*
 * Copyright (C) 2025 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.plugin;

import com.google.common.base.Preconditions;
import com.google.inject.Module;
import com.velocitypowered.api.plugin.PluginContainer;

/**
 * This event is fired when a plugin is loaded by the proxy.
 * It provides access to the plugin's container and the module that was created for it.
 * This event is typically used to perform additional setup or configuration for the plugin.
 */
public final class PluginLoadEvent {

  private final PluginContainer pluginContainer;
  private final Module module;

  public PluginLoadEvent(PluginContainer pluginContainer, Module module) {
    this.pluginContainer = Preconditions.checkNotNull(pluginContainer, "pluginContainer");
    this.module = Preconditions.checkNotNull(module, "module");
  }

  public PluginContainer getPluginContainer() {
    return pluginContainer;
  }

  public Module getModule() {
    return module;
  }
}
