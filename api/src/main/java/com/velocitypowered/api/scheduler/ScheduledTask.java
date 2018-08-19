package com.velocitypowered.api.scheduler;

/**
 * Represents a task that is scheduled to run on the proxy.
 */
public interface ScheduledTask {
    Object plugin();

    TaskStatus status();

    void cancel();
}
