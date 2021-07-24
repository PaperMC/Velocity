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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EventTest {

  public static final String CONTINUATION_TEST_THREAD_NAME = "Continuation test thread";
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
    final AtomicLong listenerAInvoked = new AtomicLong();
    final AtomicLong listenerBInvoked = new AtomicLong();
    final AtomicLong listenerCInvoked = new AtomicLong();

    eventManager.register(FakePluginManager.PLUGIN_A, TestEvent.class, event -> {
      listenerAInvoked.set(System.nanoTime());
    });
    eventManager.register(FakePluginManager.PLUGIN_B, TestEvent.class, event -> {
      listenerBInvoked.set(System.nanoTime());
    });
    eventManager.register(FakePluginManager.PLUGIN_A, TestEvent.class, event -> {
      listenerCInvoked.set(System.nanoTime());
    });

    try {
      eventManager.fire(new TestEvent()).get();
    } finally {
      eventManager.unregisterListeners(FakePluginManager.PLUGIN_A);
    }

    // Check that the order is A < B < C. Check only that A < B and B < C as B < C and A < B => A < C.
    assertTrue(listenerAInvoked.get() < listenerBInvoked.get(), "Listener B invoked before A!");
    assertTrue(listenerBInvoked.get() < listenerCInvoked.get(), "Listener C invoked before B!");
  }

  @Test
  void listenerOrderPreservedWithContinuation() throws Exception {
    final AtomicLong listenerAInvoked = new AtomicLong();
    final AtomicLong listenerBInvoked = new AtomicLong();
    final AtomicLong listenerCInvoked = new AtomicLong();

    eventManager.register(FakePluginManager.PLUGIN_A, TestEvent.class, event -> {
      listenerAInvoked.set(System.nanoTime());
    });
    eventManager.register(FakePluginManager.PLUGIN_B, TestEvent.class,
        (AwaitingEventExecutor<TestEvent>) event -> EventTask.withContinuation(continuation -> {
          new Thread(() -> {
            listenerBInvoked.set(System.nanoTime());
            continuation.resume();
          }).start();
        }));
    eventManager.register(FakePluginManager.PLUGIN_A, TestEvent.class, event -> {
      listenerCInvoked.set(System.nanoTime());
    });

    try {
      eventManager.fire(new TestEvent()).get();
    } finally {
      eventManager.unregisterListeners(FakePluginManager.PLUGIN_A);
    }

    // Check that the order is A < B < C. Check only that A < B and B < C as B < C and A < B => A < C.
    assertTrue(listenerAInvoked.get() < listenerBInvoked.get(), "Listener B invoked before A!");
    assertTrue(listenerBInvoked.get() < listenerCInvoked.get(), "Listener C invoked before B!");
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

    @Subscribe
    void firstAsync(TestEvent event) {
      result++;
      threadA = Thread.currentThread();
    }

    @Subscribe
    EventTask secondAsync(TestEvent event) {
      threadB = Thread.currentThread();
      return EventTask.async(() -> result++);
    }

    @Subscribe
    void thirdAsync(TestEvent event) {
      result++;
      threadC = Thread.currentThread();
    }
  }

  @Test
  void testContinuation() throws Exception {
    final ContinuationListener listener = new ContinuationListener();
    handleMethodListener(listener);
    assertAsyncThread(listener.threadA);
    assertAsyncThread(listener.threadB);
    assertContinuationThread(listener.threadBCustom);
    assertAsyncThread(listener.threadC);
    assertEquals(2, listener.value.get());
  }

  static final class ContinuationListener {

    @MonotonicNonNull Thread threadA;
    @MonotonicNonNull Thread threadB;
    @MonotonicNonNull Thread threadBCustom;
    @MonotonicNonNull Thread threadC;

    final AtomicInteger value = new AtomicInteger();

    @Subscribe(order = PostOrder.EARLY)
    EventTask continuation(TestEvent event) {
      threadA = Thread.currentThread();
      return EventTask.withContinuation(continuation -> {
        value.incrementAndGet();
        threadB = Thread.currentThread();
        new Thread(() -> {
          threadBCustom = Thread.currentThread();
          value.incrementAndGet();
          continuation.resume();
        }, CONTINUATION_TEST_THREAD_NAME).start();
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
    assertAsyncThread(listener.threadA);
    assertAsyncThread(listener.threadB);
    assertAsyncThread(listener.threadC);
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
    assertAsyncThread(listener.threadA);
    assertAsyncThread(listener.threadB);
    assertContinuationThread(listener.threadBCustom);
    assertAsyncThread(listener.threadC);
    assertEquals(3, listener.result.get());
  }

  static final class ContinuationParameterListener {

    @MonotonicNonNull Thread threadA;
    @MonotonicNonNull Thread threadB;
    @MonotonicNonNull Thread threadBCustom;
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
        threadBCustom = Thread.currentThread();
        result.incrementAndGet();
        continuation.resume();
      }, CONTINUATION_TEST_THREAD_NAME).start();
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