/*
 * Copyright (C) 2018-2022 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.scheduler;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a task that is scheduled to run on the proxy.
 */
public interface ScheduledTask {

  /**
   * Returns the plugin that scheduled this task.
   *
   * @return the plugin that scheduled this task
   */
  @NotNull Object plugin();

  /**
   * Returns the current status of this task.
   *
   * @return the current status of this task
   */
  TaskStatus status();

  /**
   * Cancels this task. If the task is already running, the thread in which it is running will be
   * interrupted. If the task is not currently running, Velocity will terminate it safely.
   */
  void cancel();
}
