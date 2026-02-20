/*
 * Copyright (C) 2018-2026 Velocity Contributors
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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A {@link SchedulerBackend} backed by a real {@link ScheduledExecutorService}.
 */
public class ExecutorSchedulerBackend implements SchedulerBackend {

  private final ScheduledExecutorService executor;

  /**
   * Creates a ExecutorSchedulerBackend with a default executor.
   */
  public ExecutorSchedulerBackend() {
    this(Executors.newSingleThreadScheduledExecutor(
        new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("Velocity Task Scheduler Timer")
            .build()
    ));
  }

  /**
   * Creates a ExecutorSchedulerBackend with a given executor.
   *
   * @param executor The executor to use.
   */
  public ExecutorSchedulerBackend(ScheduledExecutorService executor) {
    this.executor = checkNotNull(executor, "executor");
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
    return executor.schedule(task, delay, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
    return executor.scheduleAtFixedRate(task, initialDelay, period, unit);
  }

  @Override
  public void shutdown() {
    executor.shutdown();
  }
}
