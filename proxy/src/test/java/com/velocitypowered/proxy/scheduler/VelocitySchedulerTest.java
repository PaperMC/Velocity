/*
 * Copyright (C) 2018-2026 Velocity Contributors
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

package com.velocitypowered.proxy.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.TaskStatus;
import com.velocitypowered.proxy.scheduler.VelocityScheduler.VelocityTask;
import com.velocitypowered.proxy.testutil.FakePluginManager;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class VelocitySchedulerTest {

  @Test
  void buildTask() throws Exception {
    DeterministicSchedulerBackend backend = new DeterministicSchedulerBackend();
    VelocityScheduler scheduler = new VelocityScheduler(new FakePluginManager(), backend);

    CountDownLatch latch = new CountDownLatch(1);
    ScheduledTask task = scheduler.buildTask(FakePluginManager.PLUGIN_A, latch::countDown).schedule();

    backend.runUntilIdle(); // runs tasks due at t=0
    assertTrue(latch.await(5, TimeUnit.SECONDS));

    ((VelocityTask) task).awaitCompletion();
    assertEquals(TaskStatus.FINISHED, task.status());
  }

  @Test
  void cancelWorks() {
    DeterministicSchedulerBackend backend = new DeterministicSchedulerBackend();
    VelocityScheduler scheduler = new VelocityScheduler(new FakePluginManager(), backend);
    AtomicInteger i = new AtomicInteger(3);
    ScheduledTask task = scheduler.buildTask(FakePluginManager.PLUGIN_A, i::decrementAndGet)
        .delay(100, TimeUnit.SECONDS)
        .schedule();
    task.cancel();
    assertEquals(3, i.get());
    assertEquals(TaskStatus.CANCELLED, task.status());
  }

  @Test
  void repeatTaskWorks() throws Exception {
    DeterministicSchedulerBackend backend = new DeterministicSchedulerBackend();
    VelocityScheduler scheduler = new VelocityScheduler(new FakePluginManager(), backend);

    CountDownLatch latch = new CountDownLatch(3);
    ScheduledTask task = scheduler.buildTask(FakePluginManager.PLUGIN_A, latch::countDown)
        .delay(100, TimeUnit.MILLISECONDS)
        .repeat(100, TimeUnit.MILLISECONDS)
        .schedule();

    backend.advance(300, TimeUnit.MILLISECONDS); // triggers 3 timer firings deterministically
    assertTrue(latch.await(5, TimeUnit.SECONDS));

    task.cancel();
  }

  @Test
  void obtainTasksFromPlugin() throws Exception {
    DeterministicSchedulerBackend backend = new DeterministicSchedulerBackend();
    VelocityScheduler scheduler = new VelocityScheduler(new FakePluginManager(), backend);

    CountDownLatch runningLatch = new CountDownLatch(1);
    CountDownLatch endingLatch = new CountDownLatch(1);

    scheduler.buildTask(FakePluginManager.PLUGIN_A, task -> {
      runningLatch.countDown();
      try {
        endingLatch.await();
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      }
      task.cancel();
    }).delay(50, TimeUnit.MILLISECONDS)
        .repeat(Duration.ofMillis(5))
        .schedule();

    backend.advance(50, TimeUnit.MILLISECONDS); // run first tick only (no wall clock)
    assertTrue(runningLatch.await(5, TimeUnit.SECONDS));

    assertEquals(1, scheduler.tasksByPlugin(FakePluginManager.PLUGIN_A).size());

    endingLatch.countDown();
  }

  @Test
  void testConsumerCancel() throws Exception {
    DeterministicSchedulerBackend backend = new DeterministicSchedulerBackend();
    VelocityScheduler scheduler = new VelocityScheduler(new FakePluginManager(), backend);

    CountDownLatch latch = new CountDownLatch(1);

    ScheduledTask task = scheduler.buildTask(
        FakePluginManager.PLUGIN_B, actualTask -> {
          actualTask.cancel();
          latch.countDown();
        })
        .repeat(5, TimeUnit.MILLISECONDS)
        .schedule();

    assertEquals(TaskStatus.SCHEDULED, task.status());

    backend.runUntilIdle(); // initialDelay is 0 -> due immediately in virtual time
    assertTrue(latch.await(5, TimeUnit.SECONDS));

    assertEquals(TaskStatus.CANCELLED, task.status());
  }

  @Test
  void testConsumerEquality() throws Exception {
    DeterministicSchedulerBackend backend = new DeterministicSchedulerBackend();
    VelocityScheduler scheduler = new VelocityScheduler(new FakePluginManager(), backend);

    CountDownLatch latch = new CountDownLatch(1);

    AtomicReference<ScheduledTask> consumerTask = new AtomicReference<>();
    AtomicReference<ScheduledTask> initialTask = new AtomicReference<>();

    ScheduledTask task = scheduler.buildTask(FakePluginManager.PLUGIN_A, scheduledTask -> {
      consumerTask.set(scheduledTask);
      latch.countDown();
    }).delay(60, TimeUnit.MILLISECONDS).schedule();

    initialTask.set(task);

    backend.advance(60, TimeUnit.MILLISECONDS);
    assertTrue(latch.await(5, TimeUnit.SECONDS));

    assertEquals(consumerTask.get(), initialTask.get());
  }
}
