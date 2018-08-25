package com.velocitypowered.api.scheduler;

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
