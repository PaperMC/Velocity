/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.exception;

import com.velocitypowered.api.plugin.PluginContainer;

public class ProxyEventException extends ProxyPluginException {

  private final Class<?> eventType;

  public ProxyEventException(String message, Throwable cause, PluginContainer plugin, Class<?> eventType) {
    super(message, cause, plugin);
    this.eventType = eventType;
  }

  protected ProxyEventException(String message, Throwable cause, boolean enableSuppression,
                                boolean writableStackTrace, PluginContainer plugin, Class<?> eventType) {
    super(message, cause, enableSuppression, writableStackTrace, plugin);
    this.eventType = eventType;
  }

  public ProxyEventException(Throwable cause, PluginContainer plugin, Class<?> eventType) {
    super(cause, plugin);
    this.eventType = eventType;
  }

  public Class<?> getEventType() {
    return eventType;
  }
}
