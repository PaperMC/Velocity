package com.velocitypowered.proxy.util.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class OnceTest {
  @Test
  void uncontendedOnce() {
    AtomicInteger i = new AtomicInteger();
    Once once = new Once();
    once.run(i::incrementAndGet);
    assertEquals(1, i.get(), "Integer didn't increment");
  }

  @Test
  void contendedOnce() throws Exception {
    int threadsForTest = 25;

    ExecutorService service = Executors.newFixedThreadPool(threadsForTest);
    AtomicInteger i = new AtomicInteger();
    Once once = new Once();
    CountDownLatch latch = new CountDownLatch(threadsForTest);
    for (int i1 = 0; i1 < threadsForTest; i1++) {
      service.execute(() -> {
        once.run(i::incrementAndGet);
        latch.countDown();
      });
    }
    latch.await();
    service.shutdown();
    assertEquals(1, i.get(), "Integer is not equal to one");
  }

  @Test
  void handlesException() {
    Once once = new Once();
    assertThrows(RuntimeException.class, () -> once.run(() -> {
      throw new RuntimeException("exception");
    }));
    once.run(() -> fail("Once.run() ran twice"));
  }
}
