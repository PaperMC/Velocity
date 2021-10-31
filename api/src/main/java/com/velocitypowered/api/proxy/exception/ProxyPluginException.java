/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.exception;

import com.velocitypowered.api.plugin.PluginContainer;

public class ProxyPluginException extends ProxyException {

  private final PluginContainer plugin;

  public ProxyPluginException(String message, Throwable cause, PluginContainer plugin) {
    super(message, cause);
    this.plugin = plugin;
  }

  protected ProxyPluginException(String message, Throwable cause, boolean enableSuppression,
                                 boolean writableStackTrace, PluginContainer plugin) {
    super(message, cause, enableSuppression, writableStackTrace);
    this.plugin = plugin;
  }

  public ProxyPluginException(Throwable cause, PluginContainer plugin) {
    super(cause);
    this.plugin = plugin;
  }

  public PluginContainer getPlugin() {
    return plugin;
  }
}
