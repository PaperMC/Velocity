/*
 * Copyright (C) 2021-2023 Velocity Contributors
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

import com.google.common.reflect.TypeToken;
import com.velocitypowered.api.event.AwaitingEventExecutor;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.proxy.testutil.FakePluginManager;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Event firing tests.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EventTest {

  public static final String CONTINUATION_TEST_THREAD_NAME = "Continuation test thread";
  private final FakePluginManager pluginManager = new FakePluginManager();
  private final VelocityEventManager eventManager = new VelocityEventManager(pluginManager);

  @AfterAll
  void shutdown() throws Exception {
    pluginManager.shutdown();
  }

  static final class TestEvent {

  }

  static void assertSyncThread(final Thread thread) {
    assertEquals(Thread.currentThread(), thread);
  }

  static void assertAsyncThread(final Thread thread) {
    assertTrue(thread.getName().contains("Test Async Thread"));
  }

  static void assertContinuationThread(final Thread thread) {
    assertEquals(CONTINUATION_TEST_THREAD_NAME, thread.getName());
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
  void listenerOrderPreserved() throws Exception {
    final AtomicLong listener1Invoked = new AtomicLong();
    final AtomicLong listener2Invoked = new AtomicLong();
    final AtomicLong listener3Invoked = new AtomicLong();

    eventManager.register(FakePluginManager.PLUGIN_A, TestEvent.class, event -> {
      listener1Invoked.set(System.nanoTime());
    });
    eventManager.register(FakePluginManager.PLUGIN_B, TestEvent.class, event -> {
      listener2Invoked.set(System.nanoTime());
    });
    eventManager.register(FakePluginManager.PLUGIN_A, TestEvent.class, event -> {
      listener3Invoked.set(System.nanoTime());
    });

    try {
      eventManager.fire(new TestEvent()).get();
    } finally {
      eventManager.unregisterListeners(FakePluginManager.PLUGIN_A);
      eventManager.unregisterListeners(FakePluginManager.PLUGIN_B);
    }

    // Check that the order is A < B < C.
    assertTrue(listener1Invoked.get() < listener2Invoked.get(), "Listener B invoked before A!");
    assertTrue(listener2Invoked.get() < listener3Invoked.get(), "Listener C invoked before B!");
  }

  @Test
  void listenerOrderPreservedWithContinuation() throws Exception {
    final AtomicLong listener1Invoked = new AtomicLong();
    final AtomicLong listener2Invoked = new AtomicLong();
    final AtomicLong listener3Invoked = new AtomicLong();

    eventManager.register(FakePluginManager.PLUGIN_A, TestEvent.class, event ->
        listener1Invoked.set(System.nanoTime()));
    eventManager.register(FakePluginManager.PLUGIN_B, TestEvent.class,
        (AwaitingEventExecutor<TestEvent>) event -> EventTask.withContinuation(continuation -> {
          new Thread(() -> {
            listener2Invoked.set(System.nanoTime());
            continuation.resume();
          }).start();
        }));
    eventManager.register(FakePluginManager.PLUGIN_A, TestEvent.class, event ->
        listener3Invoked.set(System.nanoTime()));

    try {
      eventManager.fire(new TestEvent()).get();
    } finally {
      eventManager.unregisterListeners(FakePluginManager.PLUGIN_A);
      eventManager.unregisterListeners(FakePluginManager.PLUGIN_B);
    }

    // Check that the order is A < B < C.
    assertTrue(listener1Invoked.get() < listener2Invoked.get(), "Listener B invoked before A!");
    assertTrue(listener2Invoked.get() < listener3Invoked.get(), "Listener C invoked before B!");
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

    @Subscribe(async = false)
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

    @Subscribe(async = true, order = PostOrder.EARLY)
    void firstAsync(TestEvent event) {
      result++;
      threadA = Thread.currentThread();
    }

    @Subscribe
    EventTask secondAsync(TestEvent event) {
      threadB = Thread.currentThread();
      return EventTask.async(() -> result++);
    }

    @Subscribe(order = PostOrder.LATE)
    void thirdAsync(TestEvent event) {
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

    @Subscribe(order = PostOrder.EARLY, async = false)
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

    @Subscribe(order = PostOrder.LATE, async = false)
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
  void testResumeContinuationImmediately() throws Exception {
    final ResumeContinuationImmediatelyListener listener =
        new ResumeContinuationImmediatelyListener();
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

  @Test
  void testContinuationParameter() throws Exception {
    final ContinuationParameterListener listener = new ContinuationParameterListener();
    handleMethodListener(listener);
    assertSyncThread(listener.threadA);
    assertSyncThread(listener.threadB);
    assertAsyncThread(listener.threadC);
    assertEquals(3, listener.result.get());
  }

  static final class ContinuationParameterListener {

    @MonotonicNonNull Thread threadA;
    @MonotonicNonNull Thread threadB;
    @MonotonicNonNull Thread threadC;

    final AtomicInteger result = new AtomicInteger();

    @Subscribe
    void resume(TestEvent event, Continuation continuation) {
      threadA = Thread.currentThread();
      result.incrementAndGet();
      continuation.resume();
    }

    @Subscribe(order = PostOrder.LATE)
    void resumeFromCustomThread(TestEvent event, Continuation continuation) {
      threadB = Thread.currentThread();
      new Thread(() -> {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        result.incrementAndGet();
        continuation.resume();
      }).start();
    }

    @Subscribe(order = PostOrder.LAST)
    void afterCustomThread(TestEvent event, Continuation continuation) {
      threadC = Thread.currentThread();
      result.incrementAndGet();
      continuation.resume();
    }
  }

  interface FancyContinuation {

    void resume();

    void resumeWithError(Exception exception);
  }

  private static final class FancyContinuationImpl implements FancyContinuation {

    private final Continuation continuation;

    private FancyContinuationImpl(final Continuation continuation) {
      this.continuation = continuation;
    }

    @Override
    public void resume() {
      continuation.resume();
    }

    @Override
    public void resumeWithError(final Exception exception) {
      continuation.resumeWithException(exception);
    }
  }

  interface TriConsumer<A, B, C> {

    void accept(A a, B b, C c);
  }

  @Test
  void testFancyContinuationParameter() throws Exception {
    eventManager.registerHandlerAdapter(
        "fancy",
        method -> method.getParameterCount() > 1
            && method.getParameterTypes()[1] == FancyContinuation.class,
        (method, errors) -> {
          if (method.getReturnType() != void.class) {
            errors.add("method return type must be void");
          }
          if (method.getParameterCount() != 2) {
            errors.add("method must have exactly two parameters, the first is the event and "
                + "the second is the fancy continuation");
          }
        },
        new TypeToken<TriConsumer<Object, Object, FancyContinuation>>() {},
        invokeFunction -> (instance, event) ->
            EventTask.withContinuation(continuation ->
                invokeFunction.accept(instance, event, new FancyContinuationImpl(continuation))
            ));
    final FancyContinuationListener listener = new FancyContinuationListener();
    handleMethodListener(listener);
    assertEquals(1, listener.result);
  }

  static final class FancyContinuationListener {

    int result;

    @Subscribe
    void continuation(TestEvent event, FancyContinuation continuation) {
      result++;
      continuation.resume();
    }
  }
}
