package com.velocitypowered.proxy.util.except;

/**
 * A special-purpose exception thrown when we want to indicate that promise failed but do not want
 * to see a large stack trace in logs.
 */
public final class QuietPromiseException extends Exception {
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

    @Override
    public Throwable initCause(Throwable cause) {
        return this;
    }
}
