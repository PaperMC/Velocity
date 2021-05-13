/*
 * Copyright (C) 2018 Velocity Contributors
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.proxy.testutil.FakePluginManager;
import java.util.concurrent.atomic.AtomicInteger;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EventTest {

  private final VelocityEventManager eventManager =
      new VelocityEventManager(new FakePluginManager());

  @AfterAll
  void shutdown() throws Exception {
    eventManager.shutdown();
  }

  static final class TestEvent {
  }

  static void assertAsyncThread(final Thread thread) {
    assertTrue(thread.getName().contains("Velocity Async Event Executor"));
  }

  static void assertSyncThread(final Thread thread) {
    assertEquals(Thread.currentThread(), thread);
  }

  private void handleMethodListener(final Object listener) throws Exception {
    eventManager.register(FakePluginManager.PLUGIN_A, listener);
    try {
      eventManager.fire(new TestEvent()).get();
    } finally {
      eventManager.unregisterListeners(FakePluginManager.PLUGIN_A);
    }
  }

  @Test
  void testAlwaysSync() throws Exception {
    final AlwaysSyncListener listener = new AlwaysSyncListener();
    handleMethodListener(listener);
    assertSyncThread(listener.thread);
    assertEquals(1, listener.result);
  }

  static final class AlwaysSyncListener {

    @MonotonicNonNull Thread thread;
    int result;

    @Subscribe
    void sync(TestEvent event) {
      result++;
      thread = Thread.currentThread();
    }
  }

  @Test
  void testAlwaysAsync() throws Exception {
    final AlwaysAsyncListener listener = new AlwaysAsyncListener();
    handleMethodListener(listener);
    assertAsyncThread(listener.threadA);
    assertAsyncThread(listener.threadB);
    assertAsyncThread(listener.threadC);
    assertEquals(3, listener.result);
  }

  static final class AlwaysAsyncListener {

    @MonotonicNonNull Thread threadA;
    @MonotonicNonNull Thread threadB;
    @MonotonicNonNull Thread threadC;
    int result;

    @Subscribe(async = true)
    void async0(TestEvent event) {
      result++;
      threadA = Thread.currentThread();
    }

    @Subscribe
    EventTask async1(TestEvent event) {
      threadB = Thread.currentThread();
      return EventTask.async(() -> result++);
    }

    @Subscribe
    void async2(TestEvent event) {
      result++;
      threadC = Thread.currentThread();
    }
  }

  @Test
  void testSometimesAsync() throws Exception {
    final SometimesAsyncListener listener = new SometimesAsyncListener();
    handleMethodListener(listener);
    assertSyncThread(listener.threadA);
    assertSyncThread(listener.threadB);
    assertAsyncThread(listener.threadC);
    assertAsyncThread(listener.threadD);
    assertEquals(3, listener.result);
  }

  static final class SometimesAsyncListener {

    @MonotonicNonNull Thread threadA;
    @MonotonicNonNull Thread threadB;
    @MonotonicNonNull Thread threadC;
    @MonotonicNonNull Thread threadD;
    int result;

    @Subscribe(order = PostOrder.EARLY)
    void notAsync(TestEvent event) {
      result++;
      threadA = Thread.currentThread();
    }

    @Subscribe
    EventTask notAsyncUntilTask(TestEvent event) {
      threadB = Thread.currentThread();
      return EventTask.async(() -> {
        threadC = Thread.currentThread();
        result++;
      });
    }

    @Subscribe(order = PostOrder.LATE)
    void stillAsyncAfterTask(TestEvent event) {
      threadD = Thread.currentThread();
      result++;
    }
  }

  @Test
  void testContinuation() throws Exception {
    final ContinuationListener listener = new ContinuationListener();
    handleMethodListener(listener);
    assertSyncThread(listener.threadA);
    assertSyncThread(listener.threadB);
    assertAsyncThread(listener.threadC);
    assertEquals(2, listener.value.get());
  }

  static final class ContinuationListener {

    @MonotonicNonNull Thread threadA;
    @MonotonicNonNull Thread threadB;
    @MonotonicNonNull Thread threadC;

    final AtomicInteger value = new AtomicInteger();

    @Subscribe(order = PostOrder.EARLY)
    EventTask continuation(TestEvent event) {
      threadA = Thread.currentThread();
      return EventTask.withContinuation(continuation -> {
        value.incrementAndGet();
        threadB = Thread.currentThread();
        new Thread(() -> {
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          value.incrementAndGet();
          continuation.resume();
        }).start();
      });
    }

    @Subscribe(order = PostOrder.LATE)
    void afterContinuation(TestEvent event) {
      threadC = Thread.currentThread();
    }
  }

  @Test
  void testAsyncContinuation() throws Exception {
    final AsyncContinuationListener listener = new AsyncContinuationListener();
    handleMethodListener(listener);
    assertSyncThread(listener.threadA);
    assertAsyncThread(listener.threadB);
    assertAsyncThread(listener.threadC);
    assertEquals(2, listener.value.get());
  }

  static final class AsyncContinuationListener {

    @MonotonicNonNull Thread threadA;
    @MonotonicNonNull Thread threadB;
    @MonotonicNonNull Thread threadC;

    final AtomicInteger value = new AtomicInteger();

    @Subscribe(order = PostOrder.EARLY)
    EventTask continuation(TestEvent event) {
      threadA = Thread.currentThread();
      return EventTask.asyncWithContinuation(continuation -> {
        value.incrementAndGet();
        threadB = Thread.currentThread();
        new Thread(() -> {
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          value.incrementAndGet();
          continuation.resume();
        }).start();
      });
    }

    @Subscribe(order = PostOrder.LATE)
    void afterContinuation(TestEvent event) {
      threadC = Thread.currentThread();
    }
  }

  @Test
  void testResumeContinuationImmediately() throws Exception {
    final ResumeContinuationImmediatelyListener listener = new ResumeContinuationImmediatelyListener();
    handleMethodListener(listener);
    assertSyncThread(listener.threadA);
    assertSyncThread(listener.threadB);
    assertSyncThread(listener.threadC);
    assertEquals(2, listener.result);
  }

  static final class ResumeContinuationImmediatelyListener {

    @MonotonicNonNull Thread threadA;
    @MonotonicNonNull Thread threadB;
    @MonotonicNonNull Thread threadC;
    int result;

    @Subscribe(order = PostOrder.EARLY)
    EventTask continuation(TestEvent event) {
      threadA = Thread.currentThread();
      return EventTask.withContinuation(continuation -> {
        threadB = Thread.currentThread();
        result++;
        continuation.resume();
      });
    }

    @Subscribe(order = PostOrder.LATE)
    void afterContinuation(TestEvent event) {
      threadC = Thread.currentThread();
      result++;
    }
  }
}
