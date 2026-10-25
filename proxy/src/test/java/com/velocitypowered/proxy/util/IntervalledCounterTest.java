/*
 * Copyright (C) 2026 Velocity Contributors
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

package com.velocitypowered.proxy.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class IntervalledCounterTest {

  private static final long INTERVAL = TimeUnit.SECONDS.toNanos(7);

  @Test
  void sumAndRateTrackAddedCounts() {
    IntervalledCounter counter = new IntervalledCounter(INTERVAL);
    long now = 0;

    counter.updateAndAdd(700, now);
    assertEquals(700, counter.getSum());
    assertEquals(100.0, counter.getRate(), 1e-9);

    now += TimeUnit.SECONDS.toNanos(1);
    counter.updateAndAdd(1400, now);
    assertEquals(2100, counter.getSum());
    assertEquals(300.0, counter.getRate(), 1e-9);
  }

  @Test
  void dataPointsOutsideTheWindowAreEvicted() {
    IntervalledCounter counter = new IntervalledCounter(INTERVAL);
    long now = 0;

    counter.updateAndAdd(10, now);
    now += TimeUnit.SECONDS.toNanos(3);
    counter.updateAndAdd(20, now);
    assertEquals(30, counter.getSum());
    assertEquals(2, counter.totalDataPoints());

    // 7.5s after the first point: only the second point remains
    now = TimeUnit.MILLISECONDS.toNanos(7500);
    counter.updateCurrentTime(now);
    assertEquals(20, counter.getSum());
    assertEquals(1, counter.totalDataPoints());

    // 10.5s: everything has expired
    now = TimeUnit.MILLISECONDS.toNanos(10500);
    counter.updateCurrentTime(now);
    assertEquals(0, counter.getSum());
    assertEquals(0, counter.totalDataPoints());
  }

  @Test
  void storedDataPointsAreBoundedByTimeNotByCallCount() {
    IntervalledCounter counter = new IntervalledCounter(INTERVAL);

    // Simulate a flood of tiny (even zero-sized) packets arriving far faster than one per
    // millisecond for the whole window. Before coalescing was introduced, each call stored a
    // separate data point, so an attacker could grow the ring buffer without bound while staying
    // under a bytes-per-second limit.
    final long stepNanos = 100;
    final long calls = INTERVAL / stepNanos;
    long now = 0;
    for (long i = 0; i < calls; i++) {
      counter.updateAndAdd(i % 2, now);
      now += stepNanos;
    }

    long expectedSum = calls / 2;
    assertEquals(expectedSum, counter.getSum());
    // 7s window at 1ms coalescing granularity is ~7000 points; leave some slack.
    assertTrue(counter.totalDataPoints() <= 7100,
        "expected at most ~7000 data points, got " + counter.totalDataPoints());
    assertTrue(counter.totalDataPoints() >= 7000,
        "expected at least 7000 data points, got " + counter.totalDataPoints());
  }

  @Test
  void coalescedDataPointsExpireTogether() {
    IntervalledCounter counter = new IntervalledCounter(INTERVAL);

    long now = 0;
    counter.updateAndAdd(5, now);
    // Within the same 1ms bucket: merged into the previous point
    counter.updateAndAdd(7, now + 500_000L);
    assertEquals(12, counter.getSum());
    assertEquals(1, counter.totalDataPoints());

    // A new bucket starts a new point
    counter.updateAndAdd(1, now + 1_000_000L);
    assertEquals(13, counter.getSum());
    assertEquals(2, counter.totalDataPoints());

    // The merged bucket carries the timestamp of its first point and expires with it
    counter.updateCurrentTime(now + INTERVAL + 1);
    assertEquals(1, counter.getSum());
    assertEquals(1, counter.totalDataPoints());
  }
}
