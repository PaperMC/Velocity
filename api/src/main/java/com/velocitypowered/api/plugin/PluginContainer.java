/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.plugin;

import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * A wrapper around a plugin loaded by the proxy.
 */
public interface PluginContainer {

  /**
   * Returns the plugin's description.
   *
   * @return the plugin's description
   */
  PluginDescription getDescription();

  /**
   * Returns the created plugin if it is available.
   *
   * @return the instance if available
   */
  default Optional<?> getInstance() {
    return Optional.empty();
  }

  /**
   * Returns an executor service for this plugin. The executor will use a cached
   * thread pool.
   *
   * @return an {@link ExecutorService} associated with this plugin
   */
  ExecutorService getExecutorService();
}
