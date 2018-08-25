package com.velocitypowered.proxy.scheduler;

public interface Sleeper {
    void sleep(long ms) throws InterruptedException;

    Sleeper SYSTEM = Thread::sleep;
}
