package com.velocitypowered.proxy.util.concurrency;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

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
}