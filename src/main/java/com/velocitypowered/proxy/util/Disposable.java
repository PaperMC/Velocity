package com.velocitypowered.proxy.util;

/**
 * This marker interface indicates that this object should be explicitly disposed before the object can no longer be used.
 * Not disposing these objects will likely leak native resources and eventually lead to resource exhaustion.
 */
public interface Disposable {
    void dispose();
}
