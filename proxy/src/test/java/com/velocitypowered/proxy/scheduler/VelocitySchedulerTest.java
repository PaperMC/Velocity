/*
 * Copyright (C) 2018-2023 Velocity Contributors
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(value = 10, unit = TimeUnit.SECONDS)
class VelocitySchedulerTest {

    private VelocityScheduler scheduler;

    @AfterEach
    void tearDown() throws InterruptedException {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    @Test
    void buildTask() throws Exception {
        scheduler = new VelocityScheduler(new FakePluginManager());
        CountDownLatch latch = new CountDownLatch(1);
        ScheduledTask task = scheduler.buildTask(FakePluginManager.PLUGIN_A, latch::countDown)
                .schedule();
        
        assertTrue(latch.await(3, TimeUnit.SECONDS), "Task did not complete in time");
        ((VelocityTask) task).awaitCompletion();
        assertEquals(TaskStatus.FINISHED, task.status());
    }

    @Test
    void cancelWorks() throws Exception {
        scheduler = new VelocityScheduler(new FakePluginManager());
        AtomicInteger counter = new AtomicInteger(3);
        ScheduledTask task = scheduler.buildTask(FakePluginManager.PLUGIN_A, counter::decrementAndGet)
                .delay(500, TimeUnit.MILLISECONDS)
                .schedule();
        
        task.cancel();
        assertEquals(3, counter.get(), "Task should not have executed");
        assertEquals(TaskStatus.CANCELLED, task.status());
    }

    @Test
    void repeatTaskWorks() throws Exception {
        scheduler = new VelocityScheduler(new FakePluginManager());
        CountDownLatch latch = new CountDownLatch(3);
        ScheduledTask task = scheduler.buildTask(FakePluginManager.PLUGIN_A, latch::countDown)
                .delay(100, TimeUnit.MILLISECONDS)
                .repeat(100, TimeUnit.MILLISECONDS)
                .schedule();
        
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Repeated task did not run enough times");
        task.cancel();
        assertEquals(TaskStatus.CANCELLED, task.status(), "Task should be cancelled");
    }

    @Test
    void obtainTasksFromPlugin() throws Exception {
        scheduler = new VelocityScheduler(new FakePluginManager());
        CountDownLatch runningLatch = new CountDownLatch(1);
        CountDownLatch endingLatch = new CountDownLatch(1);

        ScheduledTask task = scheduler.buildTask(FakePluginManager.PLUGIN_A, scheduledTask -> {
            runningLatch.countDown();
            try {
                endingLatch.await();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            scheduledTask.cancel();
        }).delay(100, TimeUnit.MILLISECONDS)
          .repeat(50, TimeUnit.MILLISECONDS)
          .schedule();

        assertTrue(runningLatch.await(1, TimeUnit.SECONDS), "Task did not start");
        assertEquals(1, scheduler.tasksByPlugin(FakePluginManager.PLUGIN_A).size(), 
                "Task should be registered for plugin");
        
        endingLatch.countDown();
        Thread.sleep(100); // Allow time for task to cancel
        
        assertEquals(0, scheduler.tasksByPlugin(FakePluginManager.PLUGIN_A).size(),
                "Task should be removed after cancellation");
    }

    @Test
    void testConsumerCancel() throws Exception {
        scheduler = new VelocityScheduler(new FakePluginManager());
        CountDownLatch latch = new CountDownLatch(1);

        ScheduledTask task = scheduler.buildTask(
                FakePluginManager.PLUGIN_B, actualTask -> {
                    actualTask.cancel();
                    latch.countDown();
                })
                .repeat(50, TimeUnit.MILLISECONDS)
                .schedule();

        assertEquals(TaskStatus.SCHEDULED, task.status(), "Task should be scheduled initially");
        assertTrue(latch.await(1, TimeUnit.SECONDS), "Task did not execute");
        
        // Allow cancellation to propagate
        Thread.sleep(50);
        assertEquals(TaskStatus.CANCELLED, task.status(), "Task should be cancelled after execution");
    }

    @Test
    void testConsumerEquality() throws Exception {
        scheduler = new VelocityScheduler(new FakePluginManager());
        CountDownLatch latch = new CountDownLatch(1);

        AtomicReference<ScheduledTask> consumerTask = new AtomicReference<>();
        AtomicReference<ScheduledTask> initialTask = new AtomicReference<>();

        ScheduledTask task = scheduler.buildTask(FakePluginManager.PLUGIN_A, scheduledTask -> {
            consumerTask.set(scheduledTask);
            latch.countDown();
        }).delay(100, TimeUnit.MILLISECONDS).schedule();

        initialTask.set(task);
        assertTrue(latch.await(1, TimeUnit.SECONDS), "Task did not execute");
        assertEquals(consumerTask.get(), initialTask.get(), "Task references should match");
    }
}
