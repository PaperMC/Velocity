/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.exception;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.scheduler.ScheduledTask;

public class ProxySchedulerException extends ProxyPluginException {

  private final ScheduledTask task;

  public ProxySchedulerException(String message, Throwable cause, PluginContainer plugin, ScheduledTask task) {
    super(message, cause, plugin);
    this.task = task;
  }

  protected ProxySchedulerException(String message, Throwable cause, boolean enableSuppression,
                                    boolean writableStackTrace, PluginContainer plugin, ScheduledTask task) {
    super(message, cause, enableSuppression, writableStackTrace, plugin);
    this.task = task;
  }

  public ProxySchedulerException(Throwable cause, PluginContainer plugin, ScheduledTask task) {
    super(cause, plugin);
    this.task = task;
  }

  public ScheduledTask getTask() {
    return task;
  }
}
