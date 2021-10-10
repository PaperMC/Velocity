package com.velocitypowered.proxy.event;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.EventTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

public class EventTaskTest {

  @Test
  public void testResumeWhenCompleteNormal() {
    WitnessContinuation continuation = new WitnessContinuation();
    CompletableFuture<Void> completed = CompletableFuture.completedFuture(null);
    EventTask.resumeWhenComplete(completed).execute(continuation);
    assertTrue(continuation.completedSuccessfully(), "Completed future did not "
        + "complete successfully");
  }

  @Test
  public void testResumeWhenCompleteException() {
    WitnessContinuation continuation = new WitnessContinuation();
    CompletableFuture<Void> failed = CompletableFuture.failedFuture(new Throwable());
    EventTask.resumeWhenComplete(failed).execute(continuation);
    assertTrue(continuation.completedWithError(), "Failed future completed successfully");
  }

  @Test
  public void testResumeWhenCompleteFromOtherThread() throws InterruptedException {
    WitnessContinuation continuation = new WitnessContinuation();
    CountDownLatch latch = new CountDownLatch(1);
    continuation.onComplete = (ignored) -> latch.countDown();
    CompletableFuture<Void> async = CompletableFuture.supplyAsync(() -> null);
    EventTask.resumeWhenComplete(async).execute(continuation);
    latch.await();

    assertTrue(continuation.completedSuccessfully(), "Completed future did not "
        + "complete successfully");
  }

  @Test
  public void testResumeWhenFailFromOtherThread() throws InterruptedException {
    WitnessContinuation continuation = new WitnessContinuation();
    CountDownLatch latch = new CountDownLatch(1);
    continuation.onComplete = (ignored) -> latch.countDown();
    CompletableFuture<Void> async = CompletableFuture.supplyAsync(() -> {
      throw new RuntimeException();
    });
    EventTask.resumeWhenComplete(async).execute(continuation);
    latch.await();

    assertTrue(continuation.completedWithError(), "Failed future completed successfully");
  }

  @Test
  public void testResumeWhenFailFromOtherThreadComplexChain() throws InterruptedException {
    WitnessContinuation continuation = new WitnessContinuation();
    CountDownLatch latch = new CountDownLatch(1);
    continuation.onComplete = (ignored) -> latch.countDown();
    CompletableFuture<Void> async = CompletableFuture.supplyAsync(() -> null)
            .thenAccept((v) -> {
              throw new RuntimeException();
            })
            .thenCompose((v) -> CompletableFuture.completedFuture(null));
    EventTask.resumeWhenComplete(async).execute(continuation);
    latch.await();

    assertTrue(continuation.completedWithError(), "Failed future completed successfully");
  }

  /**
   * An extremely simplified implementation of {@link Continuation} for verifying the completion
   * of an operation.
   */
  private static class WitnessContinuation implements Continuation {

    private static final AtomicIntegerFieldUpdater<WitnessContinuation> STATUS_UPDATER =
        AtomicIntegerFieldUpdater.newUpdater(WitnessContinuation.class, "status");

    private static final int UNCOMPLETED = 0;
    private static final int COMPLETED = 1;
    private static final int COMPLETED_WITH_EXCEPTION = 2;

    private volatile int status = UNCOMPLETED;
    private Consumer<Throwable> onComplete;

    @Override
    public void resume() {
      if (!STATUS_UPDATER.compareAndSet(this, UNCOMPLETED, COMPLETED)) {
        throw new IllegalStateException("Continuation is already completed");
      }

      this.onComplete.accept(null);
    }

    @Override
    public void resumeWithException(Throwable exception) {
      if (!STATUS_UPDATER.compareAndSet(this, UNCOMPLETED, COMPLETED_WITH_EXCEPTION)) {
        throw new IllegalStateException("Continuation is already completed");
      }

      this.onComplete.accept(exception);
    }

    public boolean completedSuccessfully() {
      return STATUS_UPDATER.get(this) == COMPLETED;
    }

    public boolean completedWithError() {
      return STATUS_UPDATER.get(this) == COMPLETED_WITH_EXCEPTION;
    }
  }

}
