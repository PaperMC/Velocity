package com.velocitypowered.api.scheduler;

import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.IntRangeFromNonNegative;

import java.util.concurrent.TimeUnit;

/**
 * Represents a scheduler to execute tasks on the proxy.
 */
public interface Scheduler {
    /**
     * Initializes a new {@link TaskBuilder} for creating a task on the proxy.
     * @param plugin the plugin to request the task for
     * @param runnable the task to run when scheduled
     * @return the task builder
     */
    TaskBuilder buildTask(Object plugin, Runnable runnable);

    /**
     * Represents a fluent interface to schedule tasks on the proxy.
     */
    interface TaskBuilder {
        /**
         * Specifies that the task should delay its execution by the specified amount of time.
         * @param time the time to delay by
         * @param unit the unit of time for {@code time}
         * @return this builder, for chaining
         */
        TaskBuilder delay(@IntRange(from = 0) long time, TimeUnit unit);

        /**
         * Specifies that the task should continue running after waiting for the specified amount, until it is cancelled.
         * @param time the time to delay by
         * @param unit the unit of time for {@code time}
         * @return this builder, for chaining
         */
        TaskBuilder repeat(@IntRange(from = 0) long time, TimeUnit unit);

        /**
         * Clears the delay on this task.
         * @return this builder, for chaining
         */
        TaskBuilder clearDelay();

        /**
         * Clears the repeat interval on this task.
         * @return this builder, for chaining
         */
        TaskBuilder clearRepeat();

        /**
         * Schedules this task for execution.
         * @return the scheduled task
         */
        ScheduledTask schedule();
    }
}
