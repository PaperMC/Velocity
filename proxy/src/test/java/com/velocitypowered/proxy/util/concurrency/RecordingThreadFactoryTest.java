package com.velocitypowered.proxy.util.concurrency;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static org.junit.jupiter.api.Assertions.*;

class RecordingThreadFactoryTest {

    @Test
    void newThread() throws Exception {
        RecordingThreadFactory factory = new RecordingThreadFactory(Executors.defaultThreadFactory());
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch endThread = new CountDownLatch(1);
        factory.newThread(() -> {
            started.countDown();
            assertTrue(factory.currentlyInFactory());
            assertEquals(1, factory.size());
            try {
                endThread.await();
            } catch (InterruptedException e) {
                fail(e);
            }
        }).start();
        started.await();
        assertFalse(factory.currentlyInFactory());
        assertEquals(1, factory.size());
        endThread.countDown();

        // Wait a little bit to ensure the thread got shut down
        Thread.sleep(10);
        assertEquals(0, factory.size());
    }

    @Test
    void cleanUpAfterExceptionThrown() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch endThread = new CountDownLatch(1);
        CountDownLatch hasEnded = new CountDownLatch(1);
        RecordingThreadFactory factory = new RecordingThreadFactory((ThreadFactory) r -> {
            Thread t = new Thread(r);
            t.setUncaughtExceptionHandler((t1, e) -> hasEnded.countDown());
            return t;
        });
        factory.newThread(() -> {
            started.countDown();
            assertTrue(factory.currentlyInFactory());
            assertEquals(1, factory.size());
            try {
                endThread.await();
            } catch (InterruptedException e) {
                fail(e);
            }
            throw new RuntimeException("");
        }).start();
        started.await();
        assertFalse(factory.currentlyInFactory());
        assertEquals(1, factory.size());
        endThread.countDown();
        hasEnded.await();

        // Wait a little bit to ensure the thread got shut down
        Thread.sleep(10);
        assertEquals(0, factory.size());
    }
}