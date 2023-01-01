/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.scheduler;

/**
 * Enumerates all possible task statuses.
 */
public enum TaskStatus {
  /**
   * The task is scheduled and is currently running.
   */
  SCHEDULED,
  /**
   * The task was cancelled with {@link ScheduledTask#cancel()}.
   */
  CANCELLED,
  /**
   * The task has run to completion. This is applicable only for tasks without a repeat.
   */
  FINISHED
}
