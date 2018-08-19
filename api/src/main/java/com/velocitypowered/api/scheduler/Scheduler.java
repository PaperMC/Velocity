package com.velocitypowered.api.scheduler;

import java.util.concurrent.TimeUnit;

/**
 * Represents a scheduler to execute tasks on the proxy.
 */
public interface Scheduler {
    TaskBuilder buildTask(Object plugin, Runnable runnable);

    interface TaskBuilder {
        TaskBuilder delay(int time, TimeUnit unit);

        TaskBuilder repeat(int time, TimeUnit unit);

        TaskBuilder clearDelay();

        TaskBuilder clearRepeat();

        ScheduledTask schedule();
    }
}
