/*
 * Copyright (C) 2025 Velocity Contributors
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

/**
 * IntervalledCounter maintains a rolling sum of values associated with timestamps, keeping
 * only those entries that fall within a fixed time interval from the most recent timestamp.
 *
 * <p>Time values must be provided in the same unit as {@link System#nanoTime()} (nanoseconds),
 * and the configured interval is also expressed in nanoseconds. Callers are expected to
 * periodically advance the counter to the current time using {@link #updateCurrentTime()} or
 * {@link #updateCurrentTime(long)} to evict expired entries before adding new ones via
 * {@link #addTime(long)} or {@link #addTime(long, long)}.</p>
 *
 * <p>This class is not thread-safe. If multiple threads access an instance concurrently,
 * external synchronization is required.</p>
 */
@SuppressWarnings("checkstyle:WhitespaceAfter") // Not our class
public final class IntervalledCounter {

  private static final int INITIAL_SIZE = 8;

  /**
   * Ring buffer holding the timestamp (in nanoseconds) for each data point.
   */
  protected long[] times;
  /**
   * Ring buffer holding the count associated with each timestamp.
   */
  protected long[] counts;
  /**
   * The sliding window size in nanoseconds. Only entries with time >= (currentTime - interval)
   * are considered part of the window.
   */
  protected final long interval;
  /**
   * Cached lower bound of the window (in nanoseconds) after the last update.
   */
  protected long minTime;
  /**
   * Running sum of all counts currently within the window.
   */
  protected long sum;
  /**
   * Head index (inclusive) of the ring buffer.
   */
  protected int head; // inclusive
  /**
   * Tail index (exclusive) of the ring buffer.
   */
  protected int tail; // exclusive

  /**
   * Creates a new counter with the specified interval.
   *
   * @param interval the window size in nanoseconds (compatible with {@link System#nanoTime()})
   */
  public IntervalledCounter(final long interval) {
    this.times = new long[INITIAL_SIZE];
    this.counts = new long[INITIAL_SIZE];
    this.interval = interval;
  }

  /**
   * Advances the window to the current time using {@link System#nanoTime()}, evicting any
   * data points that have fallen outside of the interval and updating the running sum.
   */
  public void updateCurrentTime() {
    this.updateCurrentTime(System.nanoTime());
  }

  /**
   * Advances the window to the provided time, evicting any data points older than
   * {@code currentTime - interval} and updating the running sum.
   *
   * @param currentTime the current time in nanoseconds (as from {@link System#nanoTime()})
   */
  public void updateCurrentTime(final long currentTime) {
    long sum = this.sum;
    int head = this.head;
    final int tail = this.tail;
    final long minTime = currentTime - this.interval;

    final int arrayLen = this.times.length;

    // guard against overflow by using subtraction
    while (head != tail && this.times[head] - minTime < 0) {
      sum -= this.counts[head];
      // there are two ways we can do this:
      // 1. free the count when adding
      // 2. free it now
      // option #2
      this.counts[head] = 0;
      if (++head >= arrayLen) {
        head = 0;
      }
    }

    this.sum = sum;
    this.head = head;
    this.minTime = minTime;
  }

  /**
   * Adds a single unit at the specified timestamp, assuming the timestamp is within the current
   * window. If the timestamp is older than the current window lower bound, the value is ignored.
   * This method does not automatically advance the window; callers should invoke
   * {@link #updateCurrentTime()} or {@link #updateCurrentTime(long)} beforehand.
   *
   * @param currTime the timestamp in nanoseconds
   */
  public void addTime(final long currTime) {
    this.addTime(currTime, 1L);
  }

  /**
   * Adds {@code count} units at the specified timestamp, assuming the timestamp is within the
   * current window. If the timestamp is older than {@code minTime}, the value is ignored.
   * This method does not automatically advance the window; callers should invoke
   * {@link #updateCurrentTime()} or {@link #updateCurrentTime(long)} beforehand.
   *
   * @param currTime the timestamp in nanoseconds
   * @param count the amount to add (non-negative)
   */
  public void addTime(final long currTime, final long count) {
    // guard against overflow by using subtraction
    if (currTime - this.minTime < 0) {
      return;
    }
    int nextTail = (this.tail + 1) % this.times.length;
    if (nextTail == this.head) {
      this.resize();
      nextTail = (this.tail + 1) % this.times.length;
    }

    this.times[this.tail] = currTime;
    this.counts[this.tail] += count;
    this.sum += count;
    this.tail = nextTail;
  }

  /**
   * Convenience method that advances the window to the current time and then adds {@code count}
   * units at that time.
   *
   * @param count the amount to add (non-negative)
   */
  public void updateAndAdd(final long count) {
    final long currTime = System.nanoTime();
    this.updateCurrentTime(currTime);
    this.addTime(currTime, count);
  }

  /**
   * Convenience method that advances the window to {@code currTime} and then adds {@code count}
   * units at that time.
   *
   * @param count the amount to add (non-negative)
   * @param currTime the timestamp in nanoseconds
   */
  public void updateAndAdd(final long count, final long currTime) {
    this.updateCurrentTime(currTime);
    this.addTime(currTime, count);
  }

  /**
   * Doubles the capacity of the internal ring buffers, preserving the order of existing data.
   */
  private void resize() {
    final long[] oldElements = this.times;
    final long[] oldCounts = this.counts;
    final long[] newElements = new long[this.times.length * 2];
    final long[] newCounts = new long[this.times.length * 2];
    this.times = newElements;
    this.counts = newCounts;

    final int head = this.head;
    final int tail = this.tail;
    final int size = tail >= head ? (tail - head) : (tail + (oldElements.length - head));
    this.head = 0;
    this.tail = size;

    if (tail >= head) {
      // sequentially ordered from [head, tail)
      System.arraycopy(oldElements, head, newElements, 0, size);
      System.arraycopy(oldCounts, head, newCounts, 0, size);
    } else {
      // ordered from [head, length)
      // then followed by [0, tail)

      System.arraycopy(oldElements, head, newElements, 0, oldElements.length - head);
      System.arraycopy(oldElements, 0, newElements, oldElements.length - head, tail);

      System.arraycopy(oldCounts, head, newCounts, 0, oldCounts.length - head);
      System.arraycopy(oldCounts, 0, newCounts, oldCounts.length - head, tail);
    }
  }

  /**
   * Returns the current rate in units per second based on the rolling sum and the configured
   * interval. Specifically: {@code sum / (intervalSeconds)} where {@code intervalSeconds}
   * equals {@code interval / 1e9}.
   *
   * @return the rate in units per second for the current window
   */
  public double getRate() {
    return (double)this.sum / ((double)this.interval * 1.0E-9);
  }

  /**
   * Returns the configured interval size in nanoseconds.
   *
   * @return the interval size in nanoseconds
   */
  public long getInterval() {
    return this.interval;
  }

  /**
   * Returns the rolling sum of all counts currently within the window.
   *
   * @return the rolling sum
   */
  public long getSum() {
    return this.sum;
  }

  /**
   * Returns the number of data points currently stored in the internal ring buffer. This may be
   * less than or equal to the number of points added since older entries may have been evicted.
   *
   * @return the number of stored data points
   */
  public int totalDataPoints() {
    return this.tail >= this.head ? (this.tail - this.head) : (this.tail + (this.counts.length - this.head));
  }
}
