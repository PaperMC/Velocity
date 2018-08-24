package com.velocitypowered.proxy.scheduler;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import com.velocitypowered.api.scheduler.TaskStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class VelocityScheduler implements Scheduler {
    private final PluginManager pluginManager;
    private final ExecutorService taskService;
    private final Sleeper sleeper;
    private final Multimap<Object, ScheduledTask> tasksByPlugin = Multimaps.synchronizedListMultimap(
            Multimaps.newListMultimap(new IdentityHashMap<>(), ArrayList::new));

    public VelocityScheduler(PluginManager pluginManager, Sleeper sleeper) {
        this.pluginManager = pluginManager;
        this.sleeper = sleeper;
        this.taskService = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setDaemon(true)
                .setNameFormat("Velocity Task Scheduler - #%d").build());
    }

    @Override
    public TaskBuilder buildTask(Object plugin, Runnable runnable) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(runnable, "runnable");
        Preconditions.checkArgument(pluginManager.fromInstance(plugin).isPresent(), "plugin is not registered");
        return new TaskBuilderImpl(plugin, runnable);
    }

    public boolean shutdown() throws InterruptedException {
        for (ScheduledTask task : ImmutableList.copyOf(tasksByPlugin.values())) {
            task.cancel();
        }
        taskService.shutdown();
        return taskService.awaitTermination(10, TimeUnit.SECONDS);
    }

    private class TaskBuilderImpl implements TaskBuilder {
        private final Object plugin;
        private final Runnable runnable;
        private long delay; // ms
        private long repeat; // ms

        private TaskBuilderImpl(Object plugin, Runnable runnable) {
            this.plugin = plugin;
            this.runnable = runnable;
        }

        @Override
        public TaskBuilder delay(int time, TimeUnit unit) {
            this.delay = unit.toMillis(time);
            return this;
        }

        @Override
        public TaskBuilder repeat(int time, TimeUnit unit) {
            this.repeat = unit.toMillis(time);
            return this;
        }

        @Override
        public TaskBuilder clearDelay() {
            this.delay = 0;
            return this;
        }

        @Override
        public TaskBuilder clearRepeat() {
            this.repeat = 0;
            return this;
        }

        @Override
        public ScheduledTask schedule() {
            VelocityTask task = new VelocityTask(plugin, runnable, delay, repeat);
            taskService.execute(task);
            tasksByPlugin.put(plugin, task);
            return task;
        }
    }

    private class VelocityTask implements Runnable, ScheduledTask {
        private final Object plugin;
        private final Runnable runnable;
        private final long delay;
        private final long repeat;
        private volatile TaskStatus status;
        private Thread taskThread;

        private VelocityTask(Object plugin, Runnable runnable, long delay, long repeat) {
            this.plugin = plugin;
            this.runnable = runnable;
            this.delay = delay;
            this.repeat = repeat;
            this.status = TaskStatus.SCHEDULED;
        }

        @Override
        public Object plugin() {
            return plugin;
        }

        @Override
        public TaskStatus status() {
            return status;
        }

        @Override
        public void cancel() {
            if (status == TaskStatus.SCHEDULED) {
                status = TaskStatus.CANCELLED;
                if (taskThread != null) {
                    taskThread.interrupt();
                }
            }
        }

        @Override
        public void run() {
            taskThread = Thread.currentThread();
            if (delay > 0) {
                try {
                    sleeper.sleep(delay);
                } catch (InterruptedException e) {
                    if (status == TaskStatus.CANCELLED) {
                        onFinish();
                        return;
                    }
                }
            }

            while (status != TaskStatus.CANCELLED) {
                try {
                    runnable.run();
                } catch (Exception e) {
                    Log.logger.error("Exception in task {} by plugin {}", runnable, plugin);
                }

                if (repeat > 0) {
                    try {
                        sleeper.sleep(repeat);
                    } catch (InterruptedException e) {
                        if (status == TaskStatus.CANCELLED) {
                            break;
                        }
                    }
                } else {
                    status = TaskStatus.FINISHED;
                    break;
                }
            }

            onFinish();
        }

        private void onFinish() {
            tasksByPlugin.remove(plugin, this);
        }
    }

    private static class Log {
        private static final Logger logger = LogManager.getLogger(VelocityTask.class);
    }
}
