package com.velocitypowered.proxy.scheduler;

import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.TaskStatus;
import com.velocitypowered.proxy.testutil.FakePluginManager;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class VelocitySchedulerTest {
    // TODO: The timings here will be inaccurate on slow systems. Need to find a testing-friendly replacement for Thread.sleep()

    @Test
    void buildTask() throws Exception {
        VelocityScheduler scheduler = new VelocityScheduler(new FakePluginManager(), Sleeper.SYSTEM);
        CountDownLatch latch = new CountDownLatch(1);
        ScheduledTask task = scheduler.buildTask(FakePluginManager.PLUGIN_A, latch::countDown).schedule();
        latch.await();
        assertEquals(TaskStatus.FINISHED, task.status());
    }

    @Test
    void cancelWorks() throws Exception {
        VelocityScheduler scheduler = new VelocityScheduler(new FakePluginManager(), Sleeper.SYSTEM);
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
        VelocityScheduler scheduler = new VelocityScheduler(new FakePluginManager(), Sleeper.SYSTEM);
        CountDownLatch latch = new CountDownLatch(3);
        ScheduledTask task = scheduler.buildTask(FakePluginManager.PLUGIN_A, latch::countDown)
                .delay(100, TimeUnit.MILLISECONDS)
                .repeat(100, TimeUnit.MILLISECONDS)
                .schedule();
        latch.await();
        task.cancel();
    }

}