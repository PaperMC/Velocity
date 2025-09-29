/*
 * Copyright (C) 2021 Velocity Contributors
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
 * <p><strong>Compatibility notice:</strong> While in Velocity 3.0.0, all event handlers still
 * execute asynchronously (to preserve backwards compatibility), this will not be the case in future
 * versions of Velocity. Please prepare your code by using continuations or returning an instance
 * returned by {@link #async(Runnable)}.</p>
 */
public interface EventTask {

  /**
   * Whether this {@link EventTask} is required to be called asynchronously.
   *
   * <p>If this method returns {@code true}, the event task is guaranteed to be executed
   * asynchronously from the current thread. Otherwise, the event task may be executed on the
   * current thread or asynchronously.</p>
   *
   * @return Requires async
   */
  boolean requiresAsync();

  /**
   * Runs this event task with the given {@link Continuation}. The continuation must be notified
   * when the task is completed, either with {@link Continuation#resume()} if the task was
   * successful or {@link Continuation#resumeWithException(Throwable)} if an exception occurred.
   *
   * <p>The {@link Continuation} may only be resumed once, or an
   * {@link IllegalStateException} will be thrown.</p>
   *
   * <p>The {@link Continuation} doesn't need to be notified during the execution of this method,
   * this can happen at a later point in time and from another thread.</p>
   *
   * @param continuation The continuation
   */
  void execute(Continuation continuation);

  /**
   * Creates a basic async {@link EventTask} from the given {@link Runnable}. The task is guaranteed
   * to be executed asynchronously ({@link #requiresAsync()} always returns {@code true}).
   *
   * @param task The task
   * @return The async event task
   */
  static EventTask async(final Runnable task) {
    requireNonNull(task, "task");
    return new EventTask() {

      @Override
      public void execute(Continuation continuation) {
        task.run();
        continuation.resume();
      }

      @Override
      public boolean requiresAsync() {
        return true;
      }
    };
  }

  /**
   * Creates a continuation based {@link EventTask} from the given {@link Consumer}. The task isn't
   * guaranteed to be executed asynchronously ({@link #requiresAsync()} always returns
   * {@code false}).
   *
   * @param task The task to execute
   * @return The event task
   */
  static EventTask withContinuation(final Consumer<Continuation> task) {
    requireNonNull(task, "task");
    return new EventTask() {

      @Override
      public void execute(final Continuation continuation) {
        task.accept(continuation);
      }

      @Override
      public boolean requiresAsync() {
        return false;
      }
    };
  }

  /**
   * Creates a continuation based {@link EventTask} for the given {@link CompletableFuture}. The
   * continuation is resumed upon completion of the given {@code future}, whether it is completed
   * successfully or not.
   *
   * @param future The task to wait for
   * @return The event task
   */
  // The Error Prone annotation here is spurious. The Future is handled via the CompletableFuture
  // API, which does NOT use the traditional blocking model.
  @SuppressWarnings("FutureReturnValueIgnored")
  static EventTask resumeWhenComplete(final CompletableFuture<?> future) {
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