/*
 * Copyright (C) 2020-2023 Velocity Contributors
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

package com.velocitypowered.proxy.util.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Tests the proper functionality of {@code Once}.
 */
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
