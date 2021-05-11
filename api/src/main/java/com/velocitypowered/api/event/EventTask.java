/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Represents a task that can be returned by a {@link EventHandler} which allows event handling to
 * be suspended and resumed at a later time, and executing event handlers completely or partially
 * asynchronously.
 *
 * <p>By default will all event handlers be executed on the thread the event was posted, using
 * event tasks this behavior can be altered.</p>
 */
public abstract class EventTask {

  EventTask() {
  }

  /**
   * Whether this {@link EventTask} is required to be called asynchronously.
   *
   * <p>If this method returns {@code true}, the event task is guaranteed to be executed
   * asynchronously from the current thread. Otherwise, the event task may be executed on the
   * current thread or asynchronously.</p>
   *
   * @return Requires async
   */
  public abstract boolean requiresAsync();

  /**
   * Represents a basic {@link EventTask}. The execution of the event handlers will resume after
   * this basic task is executed using {@link #run()}.
   */
  public abstract static class Basic extends EventTask {

    /**
     * Runs the task.
     */
    public abstract void run();
  }

  /**
   * Represents an {@link EventTask} which receives a {@link Continuation} through
   * {@link #run(Continuation)}. The continuation must be notified when the task is
   * completed, either with {@link Continuation#resume()} if the task was successful or
   * {@link Continuation#resumeWithException(Throwable)} if an exception occurred.
   *
   * <p>The {@link Continuation} may only be resumed once, or an
   * {@link IllegalStateException} is expected.</p>
   *
   * <p>The {@link Continuation} doesn't need to be notified during the execution of
   * {@link #run(Continuation)}, this can happen at a later point in time and from another
   * thread.</p>
   */
  public abstract static class WithContinuation extends EventTask {

    /**
     * Runs this async task with the given continuation.
     *
     * @param continuation The continuation
     */
    public abstract void run(Continuation continuation);
  }

  /**
   * Creates a basic {@link EventTask} from the given {@link Runnable}. The task isn't guaranteed
   * to be executed asynchronously ({@link #requiresAsync()} always returns {@code false}).
   *
   * @param task The task
   * @return The event task
   */
  public static EventTask.Basic of(final Runnable task) {
    requireNonNull(task, "task");
    return new Basic() {

      @Override
      public void run() {
        task.run();
      }

      @Override
      public boolean requiresAsync() {
        return false;
      }
    };
  }

  /**
   * Creates a basic async {@link EventTask} from the given {@link Runnable}. The task is guaranteed
   * to be executed asynchronously ({@link #requiresAsync()} always returns {@code true}).
   *
   * @param task The task
   * @return The async event task
   */
  public static EventTask.Basic async(final Runnable task) {
    requireNonNull(task, "task");
    return new Basic() {

      @Override
      public void run() {
        task.run();
      }

      @Override
      public boolean requiresAsync() {
        return true;
      }
    };
  }

  /**
   * Creates an continuation based {@link EventTask} from the given {@link Consumer}. The task isn't
   * guaranteed to be executed asynchronously ({@link #requiresAsync()} always returns
   * {@code false}).
   *
   * @param task The task to execute
   * @return The event task
   */
  public static EventTask.WithContinuation withContinuation(
      final Consumer<Continuation> task) {
    requireNonNull(task, "task");
    return new WithContinuation() {

      @Override
      public void run(final Continuation continuation) {
        task.accept(continuation);
      }

      @Override
      public boolean requiresAsync() {
        return false;
      }
    };
  }

  /**
   * Creates an async continuation based {@link EventTask} from the given {@link Consumer}. The task
   * is guaranteed to be executed asynchronously ({@link #requiresAsync()} always returns
   * {@code false}).
   *
   * @param task The task to execute
   * @return The event task
   */
  public static EventTask.WithContinuation asyncWithContinuation(
      final Consumer<Continuation> task) {
    requireNonNull(task, "task");
    return new WithContinuation() {

      @Override
      public void run(final Continuation continuation) {
        task.accept(continuation);
      }

      @Override
      public boolean requiresAsync() {
        return true;
      }
    };
  }

  /**
   * Creates an continuation based {@link EventTask} for the given {@link CompletableFuture}. The
   * continuation will be notified once the given future is completed.
   *
   * @param future The task to wait for
   * @return The event task
   */
  public static EventTask.WithContinuation resumeWhenComplete(
      final CompletableFuture<?> future) {
    requireNonNull(future, "future");
    return withContinuation(continuation -> future.whenComplete((result, cause) -> {
      if (cause != null) {
        continuation.resumeWithException(cause);
      } else {
        continuation.resume();
      }
    }));
  }
}
