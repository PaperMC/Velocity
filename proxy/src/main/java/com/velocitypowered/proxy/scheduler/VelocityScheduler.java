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
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.concurrent.*;

public class VelocityScheduler implements Scheduler {
    private final PluginManager pluginManager;
    private final ExecutorService taskService;
    private final ScheduledExecutorService timerExecutionService;
    private final Multimap<Object, ScheduledTask> tasksByPlugin = Multimaps.synchronizedMultimap(
            Multimaps.newSetMultimap(new IdentityHashMap<>(), HashSet::new));

    public VelocityScheduler(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
        this.taskService = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setDaemon(true)
                .setNameFormat("Velocity Task Scheduler - #%d").build());
        this.timerExecutionService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setDaemon(true)
                .setNameFormat("Velocity Task Scheduler Timer").build());
    }

    @Override
    public TaskBuilder buildTask(Object plugin, Runnable runnable) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(runnable, "runnable");
        Preconditions.checkArgument(pluginManager.fromInstance(plugin).isPresent(), "plugin is not registered");
        return new TaskBuilderImpl(plugin, runnable);
    }

    public boolean shutdown() throws InterruptedException {
        Collection<ScheduledTask> terminating;
        synchronized (tasksByPlugin) {
            terminating = ImmutableList.copyOf(tasksByPlugin.values());
        }
        for (ScheduledTask task : terminating) {
            task.cancel();
        }
        timerExecutionService.shutdown();
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
        public TaskBuilder delay(long time, TimeUnit unit) {
            this.delay = unit.toMillis(time);
            return this;
        }

        @Override
        public TaskBuilder repeat(long time, TimeUnit unit) {
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
            tasksByPlugin.put(plugin, task);
            task.schedule();
            return task;
        }
    }

    private class VelocityTask implements Runnable, ScheduledTask {
        private final Object plugin;
        private final Runnable runnable;
        private final long delay;
        private final long repeat;
        private @Nullable ScheduledFuture<?> future;
        private volatile @Nullable Thread currentTaskThread;

        private VelocityTask(Object plugin, Runnable runnable, long delay, long repeat) {
            this.plugin = plugin;
            this.runnable = runnable;
            this.delay = delay;
            this.repeat = repeat;
        }

        public void schedule() {
            if (repeat == 0) {
                this.future = timerExecutionService.schedule(this, delay, TimeUnit.MILLISECONDS);
            } else {
                this.future = timerExecutionService.scheduleAtFixedRate(this, delay, repeat, TimeUnit.MILLISECONDS);
            }
        }

        @Override
        public Object plugin() {
            return plugin;
        }

        @Override
        public TaskStatus status() {
            if (future == null) {
                return TaskStatus.SCHEDULED;
            }

            if (future.isCancelled()) {
                return TaskStatus.CANCELLED;
            }

            if (future.isDone()) {
                return TaskStatus.FINISHED;
            }

            return TaskStatus.SCHEDULED;
        }

        @Override
        public void cancel() {
            if (future != null) {
                future.cancel(false);

                Thread cur = currentTaskThread;
                if (cur != null) {
                    cur.interrupt();
                }

                onFinish();
            }
        }

        @Override
        public void run() {
            taskService.execute(() -> {
                currentTaskThread = Thread.currentThread();
                try {
                    runnable.run();
                } catch (Exception e) {
                    Log.logger.error("Exception in task {} by plugin {}", runnable, plugin);
                } finally {
                    currentTaskThread = null;
                }
            });
        }

        private void onFinish() {
            tasksByPlugin.remove(plugin, this);
        }
    }

    private static class Log {
        private static final Logger logger = LogManager.getLogger(VelocityTask.class);
    }
}
