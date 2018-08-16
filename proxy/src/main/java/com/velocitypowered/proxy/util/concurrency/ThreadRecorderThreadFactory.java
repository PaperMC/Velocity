package com.velocitypowered.proxy.util.concurrency;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;

/**
 * Represents a {@link ThreadFactory} that records the threads it has spawned.
 */
public class ThreadRecorderThreadFactory implements ThreadFactory {
    private final ThreadFactory backing;
    private final Set<Thread> threads = ConcurrentHashMap.newKeySet();

    public ThreadRecorderThreadFactory(ThreadFactory backing) {
        this.backing = Preconditions.checkNotNull(backing, "backing");
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Preconditions.checkNotNull(runnable, "runnable");
        return backing.newThread(() -> {
            threads.add(Thread.currentThread());
            try {
                runnable.run();
            } finally {
                threads.remove(Thread.currentThread());
            }
        });
    }

    public boolean currentlyInFactory() {
        return threads.contains(Thread.currentThread());
    }

    @VisibleForTesting
    int size() {
        return threads.size();
    }
}
