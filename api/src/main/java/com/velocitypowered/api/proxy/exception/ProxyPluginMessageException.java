/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.exception;

import com.velocitypowered.api.event.connection.PluginMessageEvent;

public class ProxyPluginMessageException extends ProxyException {

  private final PluginMessageEvent event;

  public ProxyPluginMessageException(String message, PluginMessageEvent event) {
    super(message);
    this.event = event;
  }

  public ProxyPluginMessageException(String message, Throwable cause, PluginMessageEvent event) {
    super(message, cause);
    this.event = event;
  }

  protected ProxyPluginMessageException(String message, Throwable cause, boolean enableSuppression,
                                        boolean writableStackTrace, PluginMessageEvent event) {
    super(message, cause, enableSuppression, writableStackTrace);
    this.event = event;
  }

  public ProxyPluginMessageException(Throwable cause, PluginMessageEvent event) {
    super(cause);
    this.event = event;
  }

  public PluginMessageEvent getEvent() {
    return event;
  }
}
