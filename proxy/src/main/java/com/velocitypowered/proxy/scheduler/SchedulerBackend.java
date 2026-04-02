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

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Backend interface used by {@link VelocityScheduler} to schedule timer callbacks.
 *
 * <p>This is an internal abstraction that allows tests to replace the real-time scheduler
 * with a deterministic implementation.
 */
interface SchedulerBackend {

  /**
   * Schedules a task to run once after the given delay.
   *
   * @param task the task to run
   * @param delay the delay
   * @param unit the delay unit
   * @return a future representing the scheduled task
   */
  ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit);

  /**
   * Schedules a task to run at a fixed rate.
   *
   * @param task the task to run
   * @param initialDelay the initial delay
   * @param period the period between runs
   * @param unit the time unit
   * @return a future representing the scheduled task
   */
  ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit);

  /**
   * Shuts down the backend.
   */
  void shutdown();
}
