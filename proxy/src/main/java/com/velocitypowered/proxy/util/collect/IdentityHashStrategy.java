package com.velocitypowered.proxy.util.collect;

import it.unimi.dsi.fastutil.Hash.Strategy;

public final class IdentityHashStrategy<T> implements Strategy<T> {

  @SuppressWarnings("rawtypes")
  private static final IdentityHashStrategy INSTANCE = new IdentityHashStrategy();

  public static <T> Strategy<T> instance() {
    //noinspection unchecked
    return INSTANCE;
  }

  @Override
  public int hashCode(T o) {
    return System.identityHashCode(o);
  }

  @Override
  public boolean equals(T a, T b) {
    return a == b;
  }
}
