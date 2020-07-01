package com.velocitypowered.proxy.util.concurrent;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * A class that guarantees that a given initialization shall only happen once. The implementation
 * is (almost) a direct Java port of the Go {@code sync.Once} type (see the
 * <a href="https://golang.org/pkg/sync/#Once">Go documentation</a>) and thus has similar
 * semantics.
 */
public final class Once {
  private static final AtomicIntegerFieldUpdater<Once> COMPLETED_UPDATER =
      AtomicIntegerFieldUpdater.newUpdater(Once.class, "completed");

  private static final int NOT_STARTED = 0;
  private static final int COMPLETED = 1;

  private volatile int completed = NOT_STARTED;
  private final Object lock = new Object();

  /**
   * Calls {@code runnable.run()} exactly once if this instance is being called for the first time,
   * otherwise the invocation shall wait until {@code runnable.run()} completes. Future calls to
   * this method once {@code runnable.run()} completes are no-ops - a new instance should be used
   * instead.
   *
   * @param runnable the runnable to run
   */
  public void run(Runnable runnable) {
    if (COMPLETED_UPDATER.get(this) == NOT_STARTED) {
      slowRun(runnable);
    }
  }

  private void slowRun(Runnable runnable) {
    synchronized (lock) {
      if (this.completed == NOT_STARTED) {
        try {
          runnable.run();
        } finally {
          COMPLETED_UPDATER.set(this, COMPLETED);
        }
      }
    }
  }
}
