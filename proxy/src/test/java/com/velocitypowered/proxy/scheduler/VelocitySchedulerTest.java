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

package com.velocitypowered.proxy.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.TaskStatus;
import com.velocitypowered.proxy.testutil.FakePluginManager;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class VelocitySchedulerTest {
  // TODO: The timings here will be inaccurate on slow systems.

  @Test
  void buildTask() throws Exception {
    VelocityScheduler scheduler = new VelocityScheduler(new FakePluginManager());
    CountDownLatch latch = new CountDownLatch(1);
    ScheduledTask task = scheduler.buildTask(FakePluginManager.PLUGIN_A, latch::countDown)
        .schedule();
    latch.await();
    assertEquals(TaskStatus.FINISHED, task.status());
  }

  @Test
  void cancelWorks() throws Exception {
    VelocityScheduler scheduler = new VelocityScheduler(new FakePluginManager());
    AtomicInteger i = new AtomicInteger(3);
    ScheduledTask task = scheduler.buildTask(FakePluginManager.PLUGIN_A, i::decrementAndGet)
        .delay(100, TimeUnit.SECONDS)
        .schedule();
    task.cancel();
    Thread.sleep(200);
    assertEquals(3, i.get());
    assertEquals(TaskStatus.CANCELLED, task.status());
  }

  @Test
  void repeatTaskWorks() throws Exception {
    VelocityScheduler scheduler = new VelocityScheduler(new FakePluginManager());
    CountDownLatch latch = new CountDownLatch(3);
    ScheduledTask task = scheduler.buildTask(FakePluginManager.PLUGIN_A, latch::countDown)
        .delay(100, TimeUnit.MILLISECONDS)
        .repeat(100, TimeUnit.MILLISECONDS)
        .schedule();
    latch.await();
    task.cancel();
  }

}