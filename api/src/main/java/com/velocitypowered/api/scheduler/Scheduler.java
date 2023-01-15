/*
 * Copyright (C) 2018-2022 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.scheduler;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.checkerframework.common.value.qual.IntRange;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a scheduler to execute tasks on the proxy.
 */
public interface Scheduler {

  /**
   * Initializes a new {@link TaskBuilder} for creating a task on the proxy.
   *
   * @param plugin the plugin to request the task for
   * @param runnable the task to run when scheduled
   * @return the task builder
   */
  TaskBuilder buildTask(@NotNull Object plugin, @NotNull Runnable runnable);

  /**
   * Initializes a new {@link TaskBuilder} for creating a task on the proxy.
   *
   * @param plugin the plugin to request the task for
   * @param consumer the task to be run when scheduled with the capacity to cancel itself
   * @return the task builder
   */
  TaskBuilder buildTask(@NotNull Object plugin, @NotNull Consumer<ScheduledTask> consumer);

  /**
   * Get the {@link ScheduledTask} for a specific plugin.
   *
   * @param plugin the plugin object
   * @return the list of {@link ScheduledTask} corresponding to a specific plugin
   */
  @NotNull Collection<ScheduledTask> tasksByPlugin(@NotNull Object plugin);

  /**
   * Represents a fluent interface to schedule tasks on the proxy.
   */
  interface TaskBuilder {

    /**
     * Specifies that the task should delay its execution by the specified amount of time.
     *
     * @param time the time to delay by
     * @param unit the unit of time for {@code time}
     * @return this builder, for chaining
     */
    TaskBuilder delay(@IntRange(from = 0) long time, @NotNull TimeUnit unit);

    /**
     * Specifies that the task should delay its execution by the specified amount of time.
     *
     * @param duration the duration of the delay
     * @return this builder, for chaining
     */
    default TaskBuilder delay(@NotNull Duration duration) {
      return delay(duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Specifies that the task should continue running after waiting for the specified amount, until
     * it is cancelled.
     *
     * @param time the time to delay by
     * @param unit the unit of time for {@code time}
     * @return this builder, for chaining
     */
    TaskBuilder repeat(@IntRange(from = 0) long time, @NotNull TimeUnit unit);

    /**
     * Specifies that the task should continue running after waiting for the specified amount, until
     * it is cancelled.
     *
     * @param duration the duration of the delay
     * @return this builder, for chaining
     */
    default TaskBuilder repeat(@NotNull Duration duration) {
      return repeat(duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Clears the delay on this task.
     *
     * @return this builder, for chaining
     */
    TaskBuilder clearDelay();

    /**
     * Clears the repeat interval on this task.
     *
     * @return this builder, for chaining
     */
    TaskBuilder clearRepeat();

    /**
     * Schedules this task for execution.
     *
     * @return the scheduled task
     */
    ScheduledTask schedule();
  }
}
