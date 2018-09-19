package com.velocitypowered.proxy.util;

import com.google.common.base.Ticker;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RatelimiterTest {

    @Test
    void attemptZero() {
        Ratelimiter noRatelimiter = new Ratelimiter(0);
        assertTrue(noRatelimiter.attempt(InetAddress.getLoopbackAddress()));
        assertTrue(noRatelimiter.attempt(InetAddress.getLoopbackAddress()));
    }

    @Test
    void attemptOne() {
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
