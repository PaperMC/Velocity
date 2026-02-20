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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Duration;
import java.util.PriorityQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A deterministic {@link SchedulerBackend} for tests.
 *
 * <p>This backend does not use the wall clock. Tests manually advance time, and all due tasks
 * are executed deterministically on the calling thread.
 */
class DeterministicSchedulerBackend implements SchedulerBackend {

  private final Object lock = new Object();
  private final PriorityQueue<Entry> queue = new PriorityQueue<>();
  private boolean shutdown;
  private long nowNanos;
  private long seq;

  @Override
  public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
    checkNotNull(task, "task");
    checkNotNull(unit, "unit");
    return enqueue(task, unit.toNanos(delay), 0);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
    checkNotNull(task, "task");
    checkNotNull(unit, "unit");
    checkArgument(period > 0, "period must be > 0");
    return enqueue(task, unit.toNanos(initialDelay), unit.toNanos(period));
  }

  @Override
  public void shutdown() {
    synchronized (lock) {
      shutdown = true;
      queue.clear();
    }
  }

  /**
   * Runs all tasks that are due "now" without advancing time.
   */
  void runUntilIdle() {
    drainDueTasks();
  }

  /**
   * Advances virtual time and runs all tasks that become due.
   *
   * @param duration the amount of time to advance
   */
  void advance(Duration duration) {
    checkNotNull(duration, "duration");
    advance(duration.toNanos());
  }

  /**
   * Advances virtual time and runs all tasks that become due.
   *
   * @param time the time to advance
   * @param unit the unit
   */
  void advance(long time, TimeUnit unit) {
    checkNotNull(unit, "unit");
    advance(unit.toNanos(time));
  }

  private void advance(long nanos) {
    if (nanos < 0) {
      throw new IllegalArgumentException("nanos must be >= 0");
    }
    synchronized (lock) {
      nowNanos += nanos;
    }
    drainDueTasks();
  }

  private ScheduledFuture<?> enqueue(Runnable task, long delayNanos, long periodNanos) {
    synchronized (lock) {
      if (shutdown) {
        throw new java.util.concurrent.RejectedExecutionException("backend is shut down");
      }
      Entry entry = new Entry(task, nowNanos + Math.max(0, delayNanos), periodNanos, seq++);
      entry.future = new FutureImpl(entry);
      queue.add(entry);
      return entry.future;
    }
  }

  private void drainDueTasks() {
    while (true) {
      Entry entry;
      synchronized (lock) {
        entry = queue.peek();
        if (entry == null || entry.nextRunNanos > nowNanos) {
          return;
        }
        queue.poll();
      }

      // Run outside the lock to avoid deadlocks if tasks schedule more work.
      if (!entry.future.isCancelled()) {
        try {
          entry.task.run();
        } finally {
          // no-op
        }
      }

      synchronized (lock) {
        if (entry.future.isCancelled()) {
          // Cancelled tasks are not re-queued.
          continue;
        }

        if (entry.periodNanos == 0) {
          entry.future.complete();
        } else {
          // Fixed-rate semantics: next run time is based on the scheduled time, not completion time.
          entry.nextRunNanos = entry.nextRunNanos + entry.periodNanos;
          queue.add(entry);
        }
      }
    }
  }

  private final class FutureImpl implements ScheduledFuture<Object> {

    private final Entry entry;
    private final CountDownLatch completion = new CountDownLatch(1);
    private volatile boolean cancelled;
    private volatile boolean done;

    private FutureImpl(Entry entry) {
      this.entry = entry;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      synchronized (lock) {
        if (done) {
          return false;
        }
        cancelled = true;
        done = true;
        queue.remove(entry);
      }
      completion.countDown();
      return true;
    }

    @Override
    public boolean isCancelled() {
      return cancelled;
    }

    @Override
    public boolean isDone() {
      return done;
    }

    void complete() {
      if (!done) {
        done = true;
        completion.countDown();
      }
    }

    @Override
    public Object get() throws InterruptedException {
      completion.await();
      if (cancelled) {
        throw new CancellationException();
      }
      return null;
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, java.util.concurrent.TimeoutException {
      if (!completion.await(timeout, unit)) {
        throw new java.util.concurrent.TimeoutException();
      }
      if (cancelled) {
        throw new CancellationException();
      }
      return null;
    }

    @Override
    public long getDelay(TimeUnit unit) {
      synchronized (lock) {
        long remaining = Math.max(0, entry.nextRunNanos - nowNanos);
        return unit.convert(remaining, TimeUnit.NANOSECONDS);
      }
    }

    @Override
    public int compareTo(Delayed o) {
      long d1 = getDelay(TimeUnit.NANOSECONDS);
      long d2 = o.getDelay(TimeUnit.NANOSECONDS);
      return Long.compare(d1, d2);
    }
  }

  private static final class Entry implements Comparable<Entry> {

    private final Runnable task;
    private final long periodNanos;
    private final long sequence;
    private long nextRunNanos;
    private FutureImpl future;

    private Entry(Runnable task, long nextRunNanos, long periodNanos, long sequence) {
      this.task = task;
      this.nextRunNanos = nextRunNanos;
      this.periodNanos = periodNanos;
      this.sequence = sequence;
    }

    @Override
    public int compareTo(Entry other) {
      int cmp = Long.compare(this.nextRunNanos, other.nextRunNanos);
      if (cmp != 0) {
        return cmp;
      }
      return Long.compare(this.sequence, other.sequence);
    }
  }
}
