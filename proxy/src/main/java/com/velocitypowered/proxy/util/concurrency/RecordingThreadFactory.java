package com.velocitypowered.proxy.util.concurrency;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.MapMaker;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ThreadFactory;

/**
 * A {@link ThreadFactory} that records the threads it has created. Once a thread terminates, it is automatically removed
 * from the recorder.
 */
public class RecordingThreadFactory implements ThreadFactory {
    private final ThreadFactory backing;
    private final Set<Thread> threads = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());

    public RecordingThreadFactory(ThreadFactory backing) {
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
