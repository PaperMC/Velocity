package com.velocitypowered.proxy.util;

import com.google.common.base.Ticker;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class RatelimiterTest {

    @Test
    void attempt() {
        long base = System.nanoTime();
        AtomicLong extra = new AtomicLong();
        Ticker testTicker = new Ticker() {
            @Override
            public long read() {
                return base + extra.get();
            }
        };
        Ratelimiter ratelimiter = new Ratelimiter(1000, testTicker);
        assertTrue(ratelimiter.attempt(InetAddress.getLoopbackAddress()));
        assertFalse(ratelimiter.attempt(InetAddress.getLoopbackAddress()));
        extra.addAndGet(TimeUnit.SECONDS.toNanos(2));
        assertTrue(ratelimiter.attempt(InetAddress.getLoopbackAddress()));
    }
}