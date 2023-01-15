/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.scheduler;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import com.velocitypowered.api.scheduler.TaskStatus;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The Velocity "scheduler", which is actually a thin wrapper around
 * {@link ScheduledExecutorService} and a dynamically-sized {@link ExecutorService}.
 * Many plugins are accustomed to the Bukkit Scheduler model although it is not relevant
 * in a proxy context.
 */
public class VelocityScheduler implements Scheduler {

  private final PluginManager pluginManager;
  private final ExecutorService taskService;
  private final ScheduledExecutorService timerExecutionService;
  private final Multimap<Object, ScheduledTask> tasksByPlugin = Multimaps.synchronizedMultimap(
      Multimaps.newSetMultimap(new IdentityHashMap<>(), HashSet::new));

  /**
   * Initalizes the scheduler.
   *
   * @param pluginManager the Velocity plugin manager
   */
  public VelocityScheduler(PluginManager pluginManager) {
    this.pluginManager = pluginManager;
    this.taskService = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setDaemon(true)
        .setNameFormat("Velocity Task Scheduler - #%d").build());
    this.timerExecutionService = Executors
        .newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setDaemon(true)
            .setNameFormat("Velocity Task Scheduler Timer").build());
  }

  @Override
  public TaskBuilder buildTask(Object plugin, Runnable runnable) {
    checkNotNull(plugin, "plugin");
    checkNotNull(runnable, "runnable");
    checkArgument(pluginManager.fromInstance(plugin).isPresent(), "plugin is not registered");
    return new TaskBuilderImpl(plugin, runnable, null);
  }

  @Override
  public TaskBuilder buildTask(Object plugin, Consumer<ScheduledTask> consumer) {
    checkNotNull(plugin, "plugin");
    checkNotNull(consumer, "consumer");
    checkArgument(pluginManager.fromInstance(plugin).isPresent(), "plugin is not registered");
    return new TaskBuilderImpl(plugin, null, consumer);
  }

  @Override
  public @NonNull Collection<ScheduledTask> tasksByPlugin(@NonNull Object plugin) {
    checkNotNull(plugin, "plugin");
    checkArgument(pluginManager.fromInstance(plugin).isPresent(), "plugin is not registered");
    final Collection<ScheduledTask> tasks = tasksByPlugin.get(plugin);
    synchronized (tasksByPlugin) {
      return Set.copyOf(tasks);
    }
  }

  /**
   * Shuts down the Velocity scheduler.
   *
   * @return {@code true} if all tasks finished, {@code false} otherwise
   * @throws InterruptedException if the current thread was interrupted
   */
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
    private final Consumer<ScheduledTask> consumer;
    private long delay; // ms
    private long repeat; // ms

    private TaskBuilderImpl(Object plugin, Runnable runnable, Consumer<ScheduledTask> consumer) {
      this.plugin = plugin;
      this.runnable = runnable;
      this.consumer = consumer;
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
      VelocityTask task = new VelocityTask(plugin, runnable, consumer, delay, repeat);
      tasksByPlugin.put(plugin, task);
      task.schedule();
      return task;
    }
  }

  private class VelocityTask implements Runnable, ScheduledTask {

    private final Object plugin;
    private final Runnable runnable;
    private final Consumer<ScheduledTask> consumer;
    private final long delay;
    private final long repeat;
    private @Nullable ScheduledFuture<?> future;
    private volatile @Nullable Thread currentTaskThread;

    private VelocityTask(Object plugin, Runnable runnable, Consumer<ScheduledTask> consumer,
        long delay, long repeat) {
      this.plugin = plugin;
      this.runnable = runnable;
      this.consumer = consumer;
      this.delay = delay;
      this.repeat = repeat;
    }

    void schedule() {
      if (repeat == 0) {
        this.future = timerExecutionService.schedule(this, delay, TimeUnit.MILLISECONDS);
      } else {
        this.future = timerExecutionService
            .scheduleAtFixedRate(this, delay, repeat, TimeUnit.MILLISECONDS);
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
          if (runnable != null) {
            runnable.run();
          } else {
            consumer.accept(this);
          }
        } catch (Throwable e) {
          //noinspection ConstantConditions
          if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
          } else {
            String friendlyPluginName = pluginManager.fromInstance(plugin)
                .map(container -> container.getDescription().getName()
                    .orElse(container.getDescription().getId()))
                .orElse("UNKNOWN");
            Log.logger.error("Exception in task {} by plugin {}", runnable, friendlyPluginName,
                e);
          }
        } finally {
          if (repeat == 0) {
            onFinish();
          }
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
