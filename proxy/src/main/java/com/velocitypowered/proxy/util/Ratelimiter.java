package com.velocitypowered.proxy.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.net.InetAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Ratelimiter {
    private final Cache<InetAddress, Long> expiringCache;
    private final long timeoutNanos;

    public Ratelimiter(long timeoutMs) {
        this(timeoutMs, Ticker.systemTicker());
    }

    @VisibleForTesting
    Ratelimiter(long timeoutMs, Ticker ticker) {
        if (timeoutMs == 0) {
            this.timeoutNanos = timeoutMs;
            this.expiringCache = CacheBuilder.newBuilder().maximumSize(0).build();
        } else {
            this.timeoutNanos = TimeUnit.MILLISECONDS.toNanos(timeoutMs);
            this.expiringCache = CacheBuilder.newBuilder()
                    .ticker(ticker)
                    .concurrencyLevel(Runtime.getRuntime().availableProcessors())
                    .expireAfterWrite(timeoutMs, TimeUnit.MILLISECONDS)
                    .build();
        }
    }

    public boolean attempt(InetAddress address) {
        if (timeoutNanos == 0) return true;
        long expectedNewValue = System.nanoTime() + timeoutNanos;
        long last;
        try {
            last = expiringCache.get(address, () -> expectedNewValue);
        } catch (ExecutionException e) {
            // It should be impossible for this to fail.
            throw new AssertionError(e);
        }
        return expectedNewValue == last;
    }
}
