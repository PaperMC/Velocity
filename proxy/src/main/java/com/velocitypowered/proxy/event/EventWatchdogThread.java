/*
 * Copyright (C) 2021-2024 Velocity Contributors
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

package com.velocitypowered.proxy.event;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Detects deadlocks in event execution.
 */
public class EventWatchdogThread extends Thread {

  private static final Logger logger = LogManager.getLogger(EventWatchdogThread.class);

  private final Map<Thread, Long> runningEventExecutions;
  private final Set<Thread> deadlockedThreads = new HashSet<>();

  /**
   * Creates a new watchdog thread.
   *
   * @param runningEventExecutions the map of currently running event executions
   */
  public EventWatchdogThread(Map<Thread, Long> runningEventExecutions) {
    super("Velocity Event Watchdog");
    this.setDaemon(true);

    this.runningEventExecutions = runningEventExecutions;
  }

  @Override
  public void run() {
    while (!Thread.interrupted()) {
      long now = System.nanoTime();
      for (Map.Entry<Thread, Long> entry : runningEventExecutions.entrySet()) {
        Thread thread = entry.getKey();
        long executionTime = now - entry.getValue();
        // warn if the event execution took longer than 60 seconds
        if (executionTime >= 60000000000L) {
          if (!deadlockedThreads.contains(thread)) {
            deadlockedThreads.add(thread);
            logger.warn("Event execution took too long, possible deadlock detected! ({} ms)", executionTime / 1000000L);
            logger.warn("Thread: {} | State: {}", thread.getName(), thread.getState());
            logger.warn("Stack:");
            for (StackTraceElement element : thread.getStackTrace()) {
              logger.warn("  at {}", element);
            }
          }
        } else {
          deadlockedThreads.remove(thread);
        }
      }

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        break;
      }
    }
  }
}
