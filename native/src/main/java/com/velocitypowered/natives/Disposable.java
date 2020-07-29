package com.velocitypowered.natives;

import java.io.Closeable;

/**
 * This marker interface indicates that this object should be explicitly disposed before the object
 * can no longer be used. Not disposing these objects will likely leak native resources and
 * eventually lead to resource exhaustion.
 */
public interface Disposable extends Closeable {

  /**
   * Disposes this object. After this call returns, any use of this object becomes invalid. Multiple
   * calls to this function should be safe: there should be no side-effects once an object is
   * disposed.
   */
  @Override
  void close();
}
